package com.sentinelmesh.recordings;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.sentinelmesh.exceptions.RecordingSegmentStorageException;
import com.sentinelmesh.exceptions.RecordingSegmentValidationException;

@Service
public class RecordingSegmentStorageService 
{
	private final RecordingSegmentStorageProperties properties;
	private final Path storageRoot;
	
	public RecordingSegmentStorageService(RecordingSegmentStorageProperties properties)
	{
		if(properties==null)
			throw new IllegalArgumentException("Properties cannot be null");
		
		this.properties=properties;
		this.storageRoot=Paths.get(properties.getStorageRoot());
		
		if(!Files.exists(storageRoot))
			try {
				Files.createDirectories(storageRoot);
			}catch (IOException e) 
			{
				throw new RecordingSegmentStorageException("Failed to create recording storage root: " + storageRoot, e);
			}
	}
	
	public StoredRecordingSegmentFile storeRecordingSegmentFile(
			UUID deviceId,
			UUID recordingSegmentId,
			Instant segmentStartTime,
			Instant segmentEndTime,
			MultipartFile file
			)
	{
		if(deviceId==null)
			throw new IllegalArgumentException("Device Id cannot be null");
		
		if(recordingSegmentId==null)
			throw new IllegalArgumentException("RecordingSegment Id cannot be null");
		
		if(segmentStartTime==null)
			throw new IllegalArgumentException("SegmentStartTime cannot be null");
		
		if(segmentEndTime==null)
			throw new IllegalArgumentException("SegmentEndTime cannot be null");
		
		if(!segmentEndTime.isAfter(segmentStartTime))
		    throw new RecordingSegmentValidationException(
		            "SegmentEndTime must be after SegmentStartTime"
		    );
		
		if(file==null)
			throw new IllegalArgumentException("File cannot be null");
		
		String contentType=file.getContentType();
		String extension=determineFileExtension(contentType);
		Path storagePath=buildRecordingSegmentPath(
				deviceId, 
				recordingSegmentId,
				segmentStartTime,
				segmentEndTime,
				extension);
		
		String storedFilename=storagePath.getFileName().toString();
		
		long fileSizeBytes=file.getSize();
		
		if(fileSizeBytes<=0)
			throw new RecordingSegmentStorageException("File size must be greater than 0");
		
		if(fileSizeBytes > properties.getMaxFileSizeBytes())
		    throw new RecordingSegmentValidationException(
		            "File size exceeds the maximum allowed size"
		    );
		
		ensureDirectoryExists(storagePath.getParent());
		
		if(!hasRequiredFreeSpace(storagePath.getParent(), fileSizeBytes))
			throw new RecordingSegmentStorageException("Not enough space in "+storagePath.getParent());
		
		File recordingSegment=storagePath.toFile();
		try {
			file.transferTo(recordingSegment);
			
		} catch (IllegalStateException e)
		{
			throw new RecordingSegmentStorageException("Failed to store RecordingSegment file", e);
		} catch (IOException e)
		{
			throw new RecordingSegmentStorageException("Failed to store RecordingSegment file", e);
		}
		
		long duration=Duration.between(segmentStartTime, segmentEndTime).getSeconds();
		
		return new StoredRecordingSegmentFile(
				storedFilename, 
				storagePath, 
				contentType, 
				fileSizeBytes,
				Math.toIntExact(duration),
				properties.getDefaultWidth(),
				properties.getDefaultHeight());
	}
	
	public Resource loadRecordingSegmentFile(RecordingSegment recordingSegment)
	{
		if(recordingSegment==null)
			throw new IllegalArgumentException("RecordingSegment cannot be null");
		
		Path filePath=resolveStoragePath(recordingSegment.getStoragePath());
		if(!Files.exists(filePath))
			throw new RecordingSegmentStorageException("File not found: "+filePath);
		
		return new FileSystemResource(filePath);
	}
	
	public boolean deleteRecordingSegmentFile(RecordingSegment recordingSegment)
	{
		if(recordingSegment==null)
			throw new IllegalArgumentException("RecordingSegment cannot be null");
		
		Path filePath=resolveStoragePath(recordingSegment.getStoragePath());
		
		try {
			boolean deleted=Files.deleteIfExists(filePath);
			
			cleanupEmptyParentDirectories(filePath.getParent());
			
			return deleted;
		} catch (IOException e) 
		{
			throw new RecordingSegmentStorageException("Failed to delete RecordingSegment file: "+filePath, e);
		}
	}
	
	public Path buildRecordingSegmentPath(
	        UUID deviceId,
	        UUID recordingSegmentId,
	        Instant segmentStartTime,
	        Instant segmentEndTime,
	        String extension)
	{
	    if(deviceId == null)
	        throw new IllegalArgumentException("DeviceId cannot be null");

	    if(recordingSegmentId == null)
	        throw new IllegalArgumentException("RecordingSegmentId cannot be null");

	    if(segmentStartTime == null)
	        throw new IllegalArgumentException("SegmentStartTime cannot be null");
	    
	    if(segmentEndTime == null)
	        throw new IllegalArgumentException("SegmentEndTime cannot be null");
	    
	    if(!segmentEndTime.isAfter(segmentStartTime))
	        throw new RecordingSegmentValidationException(
	                "SegmentEndTime must be after SegmentStartTime"
	        );

	    if(extension == null || extension.isBlank())
	        throw new IllegalArgumentException("Extension cannot be null or blank");

	    ZonedDateTime utcStartTime =segmentStartTime.atZone(ZoneOffset.UTC);
	    ZonedDateTime utcEndTime =segmentEndTime.atZone(ZoneOffset.UTC);
	    
	    DateTimeFormatter fileTimeFormatter=DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
	    String filename=
	    		fileTimeFormatter.format(utcStartTime)
	    		+ "_"
	    		+ fileTimeFormatter.format(utcEndTime)
	    		+ "_"
	    		+ recordingSegmentId
	    		+ extension;
	    
	    return storageRoot
	            .resolve(deviceId.toString())
	            .resolve(String.valueOf(utcStartTime.getYear()))
	            .resolve(String.format("%02d", utcStartTime.getMonthValue()))
	            .resolve(String.format("%02d", utcStartTime.getDayOfMonth()))
	            .resolve(String.format("%02d", utcStartTime.getHour()))
	            .resolve(filename);
	}
	
	public void ensureDirectoryExists(Path directory)
	{
		if(!Files.exists(directory))
		{
			try {
				Files.createDirectories(directory);
			} catch (IOException e) 
			{
				throw new RecordingSegmentStorageException("Failed to create directory", e);
			}
		}
	}
	
	public String determineFileExtension(String contentType)
	{
		if(contentType==null || contentType.isBlank())
			throw new IllegalArgumentException("ContentType cannot be null or blank");
		
		if(!properties.getAllowedContentTypes().contains(contentType.toLowerCase()))
			throw new IllegalArgumentException(contentType+" is not a valid ContentType");
		
		if(contentType.equalsIgnoreCase("video/mp4"))
		{
			return ".mp4";
		}
		else
		{
			throw new RecordingSegmentStorageException("Failed to determine file extension");
		}
	}
	
	public void deleteRecordingSegmentIfSaveFailed(StoredRecordingSegmentFile storedRecordingSegment)
	{
		if(storedRecordingSegment==null)
			return;
		
		Path storagePath=storedRecordingSegment.getStoragePath();
		
		try {
			Files.deleteIfExists(storagePath);
			cleanupEmptyParentDirectories(storagePath.getParent());
		} catch (IOException e) 
		{
			System.out.println("Failed to delete recording segment after save failure: "+storagePath);
		}
	}
	
	public boolean recordingSegmentFileExists(RecordingSegment recordingSegment)
	{
	    if(recordingSegment == null)
	        throw new IllegalArgumentException("RecordingSegment cannot be null");

	    try
	    {
	        Path root = storageRoot.toAbsolutePath().normalize();
	        Path segmentPath = resolveStoragePath(recordingSegment.getStoragePath());

	        if(segmentPath.equals(root))
	            throw new RecordingSegmentStorageException(
	                    "Recording path cannot point to the storage root"
	            );

	        verifyStorageRootAccessible(root);

	        try
	        {
	            // Inspect every parent directory without following symbolic links.
	            Path current = root;

	            for(Path part : root.relativize(segmentPath.getParent()))
	            {
	                current = current.resolve(part);

	                BasicFileAttributes attributes = Files.readAttributes(
	                        current,
	                        BasicFileAttributes.class,
	                        LinkOption.NOFOLLOW_LINKS
	                );

	                if(!attributes.isDirectory())
	                    throw new RecordingSegmentStorageException(
	                            "Recording parent must be a directory, not a symbolic link or file: "
	                            + current
	                    );
	            }

	            BasicFileAttributes attributes = Files.readAttributes(
	                    segmentPath,
	                    BasicFileAttributes.class,
	                    LinkOption.NOFOLLOW_LINKS
	            );

	            if(!attributes.isRegularFile())
	                throw new RecordingSegmentStorageException(
	                        "Recording path must point to a regular file: " + segmentPath
	                );

	            return true;
	        }
	        catch(NoSuchFileException ex)
	        {
	            // A missing storage root must not be mistaken for a missing recording.
	            verifyStorageRootAccessible(root);
	            return false;
	        }
	    }
	    catch(IOException | InvalidPathException | SecurityException ex)
	    {
	        throw new RecordingSegmentStorageException(
	                "Failed to inspect recording segment file: "
	                + recordingSegment.getStoragePath(),
	                ex
	        );
	    }
	}
	
	public int deleteUnreferencedRecordingFiles(Instant cutoff, Predicate<UUID> isReferenced)
	{
		if(cutoff==null)
			throw new IllegalArgumentException("cutoff cannot be null");
		
		if(isReferenced==null)
			throw new IllegalArgumentException("isReferenced cannot be null");
		
		Path normalizedRoot=storageRoot.toAbsolutePath().normalize();
		
		Visitor visitor=new Visitor(normalizedRoot, cutoff, isReferenced);
		
		verifyStorageRootAccessible(normalizedRoot);
		
		try {
			Files.walkFileTree(normalizedRoot, Set.of(), 6, visitor);
		} catch (IOException ex) {
			// TODO Auto-generated catch block
			throw new RecordingSegmentStorageException("Error performing walkFileTree", ex);
		}
		
		return visitor.getDeletedCount();
	}
	
	private Path resolveStoragePath(String storagePath)
	{
		if(storagePath==null || storagePath.isBlank())
		{
			throw new RecordingSegmentStorageException("Storage path cannot be null or blank");
		}
		
		Path root=storageRoot.toAbsolutePath().normalize();
		Path resolved=Paths.get(storagePath).toAbsolutePath().normalize();
		
		if(!resolved.startsWith(root))
		{
			throw new RecordingSegmentStorageException("Invalid RecordingSegment storage path: "
					+ "path escapes storage root");
		}
		
		return resolved;
	}

	private void verifyStorageRootAccessible(Path root)
	{
	    try
	    {
	        BasicFileAttributes attributes = Files.readAttributes(
	                root,
	                BasicFileAttributes.class,
	                LinkOption.NOFOLLOW_LINKS
	        );

	        if(!attributes.isDirectory())
	            throw new RecordingSegmentStorageException(
	                    "Storage root must be a directory and cannot be a symbolic link: "
	                    + root
	            );

	        try(DirectoryStream<Path> ignored = Files.newDirectoryStream(root))
	        {
	            // Successfully opening the directory verifies access.
	        }
	    }
	    catch(IOException | SecurityException ex)
	    {
	        throw new RecordingSegmentStorageException(
	                "Recording storage root is unavailable: " + root,
	                ex
	        );
	    }
	}
	
	private void cleanupEmptyParentDirectories(Path directory)
	{
		if(directory==null)
			return;
		
		Path root=storageRoot.toAbsolutePath().normalize();
		Path current=directory.toAbsolutePath().normalize();
		
		while(current!=null
				&& current.startsWith(root)
				&& !current.equals(root))
		{		
			Path probe=current;
			
			while(probe!=null && !probe.equals(root))
			{
				if(Files.isSymbolicLink(probe))
					return;
				
				probe=probe.getParent();
			}
			
			try
			{
				if(Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS))
				{
					Files.deleteIfExists(current);
				}
			}
			catch(DirectoryNotEmptyException ex)
			{
				break;
			}
			catch(IOException ex)
			{
				ex.printStackTrace();
				break;
			}
			
			current=current.getParent();
		}
	}
	
	private Optional<UUID> extractManagedRecordingSegmentId(Path path)
	{
	    if(path == null)
	        throw new IllegalArgumentException("path cannot be null");

	    Path normalizedRoot = storageRoot.toAbsolutePath().normalize();
	    Path normalizedPath = path.toAbsolutePath().normalize();

	    if(!normalizedPath.startsWith(normalizedRoot))
	        return Optional.empty();

	    Path relativePath = normalizedRoot.relativize(normalizedPath);

	    if(relativePath.getNameCount() != 6)
	        return Optional.empty();

	    String filename = relativePath.getFileName().toString();
	    String extension = ".mp4";

	    if(!filename.endsWith(extension))
	        return Optional.empty();

	    String filenameWithoutExtension =
	            filename.substring(0, filename.length() - extension.length());

	    String[] parts = filenameWithoutExtension.split("_", -1);

	    if(parts.length != 3)
	        return Optional.empty();

	    DateTimeFormatter formatter = DateTimeFormatter
	            .ofPattern("uuuuMMdd'T'HHmmss'Z'")
	            .withResolverStyle(ResolverStyle.STRICT);

	    try
	    {
	        UUID deviceId = UUID.fromString(relativePath.getName(0).toString());
	        UUID segmentId = UUID.fromString(parts[2]);

	        Instant start = LocalDateTime.parse(parts[0], formatter)
	                .toInstant(ZoneOffset.UTC);
	        Instant end = LocalDateTime.parse(parts[1], formatter)
	                .toInstant(ZoneOffset.UTC);

	        if(!end.isAfter(start))
	            return Optional.empty();

	        Path expectedPath = buildRecordingSegmentPath(
	                deviceId, segmentId, start, end, extension
	        ).toAbsolutePath().normalize();

	        return normalizedPath.equals(expectedPath)
	                ? Optional.of(segmentId)
	                : Optional.empty();
	    }
	    catch(IllegalArgumentException | DateTimeParseException ex)
	    {
	        return Optional.empty();
	    }
	}
	
	private boolean isManagedRecordingDirectory(Path directory)
	{
		if(directory==null)
			return false;
		
		Path normalizedRoot=storageRoot.toAbsolutePath().normalize();
		Path normalizedDirectory=directory.toAbsolutePath().normalize();
		
		if(!normalizedDirectory.startsWith(normalizedRoot)
				|| normalizedDirectory.equals(normalizedRoot))
			return false;
		
		Path relativeRoot=normalizedRoot.relativize(normalizedDirectory);
		
		String[] parts=new String[relativeRoot.getNameCount()];
		
		for(int i=0; i<parts.length; i++)
		{
			parts[i]=relativeRoot.getName(i).toString();
		}

	    if(parts.length<1 || parts.length>5)
	        return false;
	    
	    int year = -1;
		int month=-1;
		int day=-1;
	    
	    for(int i=0; i<parts.length; i++)
	    {
	    	try
	    	{	    		
	    		if(i==0)
		    	{
		    		UUID deviceId=UUID.fromString(parts[i]);
		    		
		    		if(!parts[i].equals(deviceId.toString()))
		    	    {
		    	    	return false;
		    	    }
		    	}
		    	else if(i==1)
		    	{
		    		year=Integer.parseInt(parts[i]);
	    	    	
	    	    	if(!Year.of(year).toString().equals(parts[i]))
	    		    {
	    		    	return false;
	    		    }
		    	}
		    	else if(i==2)
		    	{
		    		month=Integer.parseInt(parts[i]);
	    	    	
	    	    	if(parts[i].length()!=2
	    	    			|| (month<1 || month>12)
	    	    			|| ((parts[i].charAt(0)<'0' || parts[i].charAt(0)>'9')
	    	    					|| (parts[i].charAt(1)<'0' || parts[i].charAt(1)>'9')))
	    		    {
	    		    	return false;
	    		    }
		    	}
		    	else if(i==3)
		    	{
		    		day=Integer.parseInt(parts[i]);
		    		
		    		LocalDate.of(year, month, day);
		    		
		    		if(parts[i].length()!=2
	    	    			|| (day<1 || day>31)
	    	    			|| ((parts[i].charAt(0)<'0' || parts[i].charAt(0)>'9')
	    	    					|| (parts[i].charAt(1)<'0' || parts[i].charAt(1)>'9')))
		    	    {
		    	    	return false;
		    	    }
		    	}
		    	else if(i==4)
		    	{
		    		int hour=Integer.parseInt(parts[i]);
	    	    	
	    	    	if(parts[i].length()!=2
	    	    			|| (hour<0 || hour>23)
	    	    			|| ((parts[i].charAt(0)<'0' || parts[i].charAt(0)>'9')
	    	    					|| (parts[i].charAt(1)<'0' || parts[i].charAt(1)>'9')))
	    		    {
	    		    	return false;
	    		    }
		    	}
	    	}
	    	catch(IllegalArgumentException | DateTimeException ex)
	    	{
	    		return false;
	    	}
	    }
	    
	    return true;
	}
	
	private long getUsableSpaceBytes(Path directory)
	{
		try
		{
			return Files.getFileStore(directory).getUsableSpace();
		}
		catch(IOException ex)
		{
			throw new RecordingSegmentStorageException("Cannot get usable bytes", ex);
		}
	}
	
	private boolean hasRequiredFreeSpace(Path directory, long incomingBytes)
	{
		if(directory==null)
			throw new IllegalArgumentException("directory cannot be null");
		
		if(incomingBytes<0)
			throw new IllegalArgumentException("incomingBytes cannot be negative");
		
		long reserveBytes=properties.getMinimumFreeSpaceBytes();
		long usableBytes=getUsableSpaceBytes(directory);
		if(usableBytes>=reserveBytes && incomingBytes<=usableBytes-reserveBytes)
			return true;
		
		return false;
	}
	
	private class Visitor extends SimpleFileVisitor<Path>
	{
		private int deletedCount=0;
		private Path root;
		private Instant cutoff;
		private Predicate<UUID> isReferenced;
		
		private Visitor(Path root, Instant cutoff, Predicate<UUID> isReferenced)
		{
			this.root=root;
			this.cutoff=cutoff;
			this.isReferenced=isReferenced;
		}
		
		@Override
		public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attributes)
		{
			if(path.equals(root) || isManagedRecordingDirectory(path))
			{
				return FileVisitResult.CONTINUE;
			}
			
			return FileVisitResult.SKIP_SUBTREE;
		}
		
		@Override
		public FileVisitResult visitFile(Path path, BasicFileAttributes attributes)
				throws IOException
		{
			if(!attributes.isRegularFile())
			{
				return FileVisitResult.CONTINUE;
			}
			
			Optional<UUID> segmentId=extractManagedRecordingSegmentId(path);
			
			if(segmentId.isEmpty())
			{
				return FileVisitResult.CONTINUE;
			}
			
			Instant lastModifiedTime=attributes.lastModifiedTime().toInstant();
			
			if(!lastModifiedTime.isBefore(cutoff))
			{
				return FileVisitResult.CONTINUE;
			}
			
			Path traversalPath=root;
			try {
				BasicFileAttributes partialPathAttributes=Files.readAttributes(
						traversalPath, 
						BasicFileAttributes.class, 
						LinkOption.NOFOLLOW_LINKS);
				
				if(!partialPathAttributes.isDirectory())
				{
					return FileVisitResult.CONTINUE;
				}
			}
			catch (IOException ex) 
			{
				throw ex;
			}
			
			for(Path component : root.relativize(path.getParent()))
			{
				traversalPath=traversalPath.resolve(component);
				
				try {
					BasicFileAttributes partialPathAttributes=Files.readAttributes(
							traversalPath, 
							BasicFileAttributes.class, 
							LinkOption.NOFOLLOW_LINKS);
					
					if(!partialPathAttributes.isDirectory())
					{
						return FileVisitResult.CONTINUE;
					}
				}
				catch(NoSuchFileException ex)
				{
					return FileVisitResult.CONTINUE;
				}
				catch (IOException ex) 
				{
					throw ex;
				}
			}
			
			try
			{
				BasicFileAttributes pathAttributes=Files.readAttributes(
						path, 
						BasicFileAttributes.class, 
						LinkOption.NOFOLLOW_LINKS);
				
				if(!pathAttributes.isRegularFile())
				{
					return FileVisitResult.CONTINUE;
				}
				
				if(!pathAttributes.lastModifiedTime().toInstant().isBefore(cutoff))
				{
					return FileVisitResult.CONTINUE;
				}
			}
			catch(NoSuchFileException ex)
			{
				return FileVisitResult.CONTINUE;
			}
			
			if(isReferenced.test(segmentId.get()))
			{
				return FileVisitResult.CONTINUE;
			}
			
			if(Files.deleteIfExists(path))
			{
				deletedCount++;
			}
			
			return FileVisitResult.CONTINUE;
		}
		
		@Override
		public FileVisitResult visitFileFailed(Path path, IOException ex) 
				throws IOException
		{			
			if(ex instanceof NoSuchFileException && !path.equals(root))
			{
				return FileVisitResult.CONTINUE;
			}
			
			throw ex;
		}
		
		@Override
		public FileVisitResult postVisitDirectory(Path path, IOException ex)
		throws IOException
		{
			if(ex!=null)
			{
				throw ex;
			}
			
			if(path.equals(root))
			{
				return FileVisitResult.CONTINUE;
			}
			
			if(!isManagedRecordingDirectory(path))
			{
				return FileVisitResult.CONTINUE;
			}
			
			Path traversalPath=root;
			try {
				BasicFileAttributes partialPathAttributes=Files.readAttributes(
						traversalPath, 
						BasicFileAttributes.class, 
						LinkOption.NOFOLLOW_LINKS);
				
				if(!partialPathAttributes.isDirectory())
				{
					return FileVisitResult.CONTINUE;
				}
			}
			catch (IOException e) 
			{
				throw e;
			}
			
			for(Path component : root.relativize(path))
			{
				traversalPath=traversalPath.resolve(component);
				
				try {
					BasicFileAttributes partialPathAttributes=Files.readAttributes(
							traversalPath, 
							BasicFileAttributes.class, 
							LinkOption.NOFOLLOW_LINKS);
					
					if(!partialPathAttributes.isDirectory())
					{
						return FileVisitResult.CONTINUE;
					}
				}
				catch(NoSuchFileException e)
				{
					return FileVisitResult.CONTINUE;
				}
				catch (IOException e) 
				{
					throw e;
				}
			}
			
			try
			{
				Files.deleteIfExists(path);
			}
			catch(DirectoryNotEmptyException e)
			{
				return FileVisitResult.CONTINUE;
			}
			return FileVisitResult.CONTINUE;
		}
		
		public int getDeletedCount()
		{
			return deletedCount;
		}
	}
}