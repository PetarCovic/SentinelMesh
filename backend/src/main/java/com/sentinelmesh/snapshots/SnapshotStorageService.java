package com.sentinelmesh.snapshots;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.sentinelmesh.exceptions.SnapshotStorageException;

@Service
public class SnapshotStorageService 
{
	private final SnapshotStorageProperties properties;
	private final Path storageRoot;
	
	public SnapshotStorageService(SnapshotStorageProperties properties)
	{
		if(properties==null)
			throw new IllegalArgumentException("Properties cannot be null");
		
		this.properties=properties;
		this.storageRoot=Paths.get(properties.getStorageRoot()).toAbsolutePath().normalize();
		
		if(!Files.exists(storageRoot))
			try {
				Files.createDirectories(storageRoot);
			} catch (IOException e) 
		{
				throw new SnapshotStorageException("File not Found: "+storageRoot.toString());
			}
	}
	
	public StoredSnapshotFile storeSnapshotFile(
			UUID deviceId,
			UUID eventId,
			UUID snapshotId,
			MultipartFile file
			)
	{
		if(deviceId==null)
			throw new IllegalArgumentException("Device Id cannot be null");
		
		if(eventId==null)
			throw new IllegalArgumentException("Event Id cannot be null");
		
		if(snapshotId==null)
			throw new IllegalArgumentException("Snapshot Id cannot be null");
		
		if(file==null)
			throw new IllegalArgumentException("File cannot be null");
		
		String contentType=file.getContentType();
		String extension=determineFileExtension(contentType);
		String storedFilename=snapshotId+extension;
		Path storagePath=buildSnapshotPath(deviceId, eventId, snapshotId, extension);
		
		long fileSizeBytes=file.getSize();
		
		if(fileSizeBytes<=0)
			throw new SnapshotStorageException("File size must be greater than 0");
		
		if(fileSizeBytes>properties.getMaxFileSizeBytes())
			throw new SnapshotStorageException("File size is too big");
		
		ensureDirectoryExists(storagePath.getParent());
		
		File snapshot=storagePath.toFile();
		BufferedImage image;
		try {
			file.transferTo(snapshot);
			image=ImageIO.read(snapshot);
			
			
		} catch (IllegalStateException e)
		{
			throw new SnapshotStorageException("Failed to store snapshot file");
		} catch (IOException e)
		{
			throw new SnapshotStorageException("Failed to store snapshot file");
		}
		
		return new StoredSnapshotFile(
				storedFilename, 
				storagePath, 
				contentType, 
				fileSizeBytes, 
				image.getWidth(),
				image.getHeight());
	}
	
	public Resource loadSnapshotFile(Snapshot snapshot)
	{
		Path filePath=resolveStoragePath(snapshot.getStoragePath());
		if(!Files.exists(filePath))
			throw new SnapshotStorageException("File not found: "+filePath);
		
		return new FileSystemResource(filePath);
	}
	
	public boolean deleteSnapshotFile(Snapshot snapshot)
	{
		Path filePath=resolveStoragePath(snapshot.getStoragePath());
		
		try {
			return Files.deleteIfExists(filePath);
		} catch (IOException e) 
		{
			throw new SnapshotStorageException("Failed to delete snapshot file: "+filePath);
		}
	}
	
	public Path buildSnapshotPath(UUID deviceId, UUID eventId, UUID snapshotId, String extension)
	{
		return storageRoot
				.resolve(deviceId.toString())
				.resolve(eventId.toString())
				.resolve(snapshotId.toString()+extension);
	}
	
	public void ensureDirectoryExists(Path directory)
	{
		if(!Files.exists(directory))
		{
			try {
				Files.createDirectories(directory);
			} catch (IOException e) 
			{
				throw new SnapshotStorageException("Failed to create directory");
			}
		}
	}
	
	public String determineFileExtension(String contentType)
	{
		if(contentType==null || contentType.isBlank())
			throw new IllegalArgumentException("ContentType cannot be null or blank");
		
		if(!properties.getAllowedContentTypes().contains(contentType.toLowerCase()))
			throw new IllegalArgumentException(contentType+" is not a valid ContentType");
		
		if(contentType.equalsIgnoreCase("image/jpeg"))
		{
			return ".jpg";
		}
		else if(contentType.equalsIgnoreCase("image/png"))
		{
			return ".png";
		}
		else
		{
			throw new SnapshotStorageException("Failed to determine file extension");
		}
	}
	
	private Path resolveStoragePath(String storagePath)
	{
		if(storagePath==null || storagePath.isBlank())
		{
			throw new SnapshotStorageException("Storage path cannot be null or blank");
		}
		
		Path root=storageRoot.toAbsolutePath().normalize();
		Path resolved=Paths.get(storagePath).toAbsolutePath().normalize();
		
		if(!resolved.startsWith(root))
		{
			throw new SnapshotStorageException("Invalid snapshot storage path: path escapes storage root");
		}
		
		return resolved;
	}
	
	public void deleteSnapshotIfSaveFailed(StoredSnapshotFile storedSnapshot)
	{
		if(storedSnapshot==null)
			return;
		
		Path storagePath=storedSnapshot.getStoragePath();
		
		try {
			Files.deleteIfExists(storagePath);
		} catch (IOException e) 
		{
			System.out.println("File does not exist at path: "+storagePath.toString());
		}
	}
}