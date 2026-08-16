package com.sentinelmesh.recordings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

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
			return Files.deleteIfExists(filePath);
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
	
	public void deleteRecordingSegmentIfSaveFailed(StoredRecordingSegmentFile storedRecordingSegment)
	{
		if(storedRecordingSegment==null)
			return;
		
		Path storagePath=storedRecordingSegment.getStoragePath();
		
		try {
			Files.deleteIfExists(storagePath);
		} catch (IOException e) 
		{
			System.out.println("Failed to delete recording segment after save failure: "+storagePath);
		}
	}
}