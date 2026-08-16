package com.sentinelmesh.clips;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.sentinelmesh.exceptions.VideoClipStorageException;

@Service
public class VideoClipStorageService 
{
	private final VideoClipStorageProperties properties;
	private final Path storageRoot;
	
	public VideoClipStorageService(VideoClipStorageProperties properties)
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
				throw new VideoClipStorageException("File not Found: "+storageRoot.toString(), e);
			}
	}
	
	public StoredVideoClipFile storeVideoClipFile(
			UUID deviceId,
			UUID eventId,
			UUID videoClipId,
			MultipartFile file
			)
	{
		if(deviceId==null)
			throw new IllegalArgumentException("Device Id cannot be null");
		
		if(eventId==null)
			throw new IllegalArgumentException("Event Id cannot be null");
		
		if(videoClipId==null)
			throw new IllegalArgumentException("VideoClip Id cannot be null");
		
		if(file==null)
			throw new IllegalArgumentException("File cannot be null");
		
		String contentType=file.getContentType();
		String extension=determineFileExtension(contentType);
		String storedFilename=videoClipId+extension;
		Path storagePath=buildVideoClipPath(deviceId, eventId, videoClipId, extension);
		
		long fileSizeBytes=file.getSize();
		
		if(fileSizeBytes<=0)
			throw new VideoClipStorageException("File size must be greater than 0");
		
		if(fileSizeBytes>properties.getMaxFileSizeBytes())
			throw new VideoClipStorageException("File size is too big");
		
		ensureDirectoryExists(storagePath.getParent());
		
		File videoClip=storagePath.toFile();
		try {
			file.transferTo(videoClip);
			
		} catch (IllegalStateException e)
		{
			throw new VideoClipStorageException("Failed to store VideoClip file", e);
		} catch (IOException e)
		{
			throw new VideoClipStorageException("Failed to store VideoClip file", e);
		}
		
		return new StoredVideoClipFile(
				storedFilename, 
				storagePath, 
				contentType, 
				fileSizeBytes,
				properties.getDefaultDurationSeconds(),
				properties.getDefaultWidth(),
				properties.getDefaultHeight());
	}
	
	public Resource loadVideoClipFile(VideoClip videoClip)
	{
		if(videoClip==null)
			throw new IllegalArgumentException("VideoClip cannot be null");
		
		Path filePath=resolveStoragePath(videoClip.getStoragePath());
		if(!Files.exists(filePath))
			throw new VideoClipStorageException("File not found: "+filePath);
		
		return new FileSystemResource(filePath);
	}
	
	public boolean deleteVideoClipFile(VideoClip videoClip)
	{
		if(videoClip==null)
			throw new IllegalArgumentException("VideoClip cannot be null");
		
		Path filePath=resolveStoragePath(videoClip.getStoragePath());
		
		try {
			return Files.deleteIfExists(filePath);
		} catch (IOException e) 
		{
			throw new VideoClipStorageException("Failed to delete VideoClip file: "+filePath);
		}
	}
	
	public Path buildVideoClipPath(UUID deviceId, UUID eventId, UUID videoClipId, String extension)
	{
		return storageRoot
				.resolve(deviceId.toString())
				.resolve(eventId.toString())
				.resolve(videoClipId.toString()+extension);
	}
	
	public void ensureDirectoryExists(Path directory)
	{
		if(!Files.exists(directory))
		{
			try {
				Files.createDirectories(directory);
			} catch (IOException e) 
			{
				throw new VideoClipStorageException("Failed to create directory", e);
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
			throw new VideoClipStorageException("Failed to determine file extension");
		}
	}
	
	private Path resolveStoragePath(String storagePath)
	{
		if(storagePath==null || storagePath.isBlank())
		{
			throw new VideoClipStorageException("Storage path cannot be null or blank");
		}
		
		Path root=storageRoot.toAbsolutePath().normalize();
		Path resolved=Paths.get(storagePath).toAbsolutePath().normalize();
		
		if(!resolved.startsWith(root))
		{
			throw new VideoClipStorageException("Invalid videoClip storage path: "
					+ "path escapes storage root");
		}
		
		return resolved;
	}
	
	public void deleteVideoClipIfSaveFailed(StoredVideoClipFile storedVideoClip)
	{
		if(storedVideoClip==null)
			return;
		
		Path storagePath=storedVideoClip.getStoragePath();
		
		try {
			Files.deleteIfExists(storagePath);
		} catch (IOException e) 
		{
			System.out.println("Failed to delete video clip after save failure: "+storagePath);
		}
	}
}
