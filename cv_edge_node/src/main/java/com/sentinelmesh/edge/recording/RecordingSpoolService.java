package com.sentinelmesh.edge.recording;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.UUID;

import com.sentinelmesh.edge.exceptions.RecordingSpoolException;
import com.sentinelmesh.edge.util.FileUtil;

public class RecordingSpoolService 
{
	private final RecordingSpoolDirectories spoolDirectories;
	private final RecordingSpoolMetadataStore metadataStore;
	
	public RecordingSpoolService(
			RecordingSpoolDirectories spoolDirectories,
			RecordingSpoolMetadataStore metadataStore
			)
	{
		if(spoolDirectories==null)
			throw new IllegalArgumentException("SpoolDirectories cannot be null");
		
		if(metadataStore==null)
			throw new IllegalArgumentException("MetadataStore cannot be null");
		
		this.spoolDirectories=spoolDirectories;
		this.metadataStore=metadataStore;
	}
	
	public synchronized RecordingSpoolEntry moveToPending(RecordingSpoolEntry entry)
	{
		if(entry==null)
			throw new IllegalArgumentException("RecordingSpoolEntry cannot be null");
		
		Path sourceVideoPath=entry.getVideoPath();
		Path targetVideoPath=spoolDirectories.getPending()
				.resolve(entry.getVideoPath().getFileName()).normalize();
		
		moveFile(sourceVideoPath, targetVideoPath);
		
		RecordingSpoolEntry newEntry=entry.markPending(targetVideoPath);

		try
		{
			metadataStore.save(newEntry);
		}
		catch(RuntimeException metadataException)
		{
			try
			{
				moveFile(targetVideoPath, sourceVideoPath);
			}
			catch(RuntimeException rollbackException)
			{
				metadataException.addSuppressed(rollbackException);
			}
			
			throw metadataException;
		}
		metadataStore.delete(metadataStore.getMetadataPath(sourceVideoPath));
		
		return newEntry;
	}
	
	public synchronized RecordingSpoolEntry moveToUploading(RecordingSpoolEntry entry)
	{
		if(entry==null)
			throw new IllegalArgumentException("RecordingSpoolEntry cannot be null");
		
		Path sourceVideoPath=entry.getVideoPath();
		Path targetVideoPath=spoolDirectories.getUploading()
				.resolve(entry.getVideoPath().getFileName()).normalize();
		
		moveFile(sourceVideoPath, targetVideoPath);
		
		RecordingSpoolEntry newEntry=entry.markUploading(targetVideoPath);
		
		try
		{
			metadataStore.save(newEntry);
		}
		catch(RuntimeException metadataException)
		{
			try
			{
				moveFile(targetVideoPath, sourceVideoPath);
			}
			catch(RuntimeException rollbackException)
			{
				metadataException.addSuppressed(rollbackException);
			}
			
			throw metadataException;
		}
		metadataStore.delete(metadataStore.getMetadataPath(sourceVideoPath));
		
		return newEntry;
	}
	
	public synchronized RecordingSpoolEntry returnToPending(RecordingSpoolEntry entry, String error)
	{
		if(entry==null)
			throw new IllegalArgumentException("RecordingSpoolEntry cannot be null");
		
		if(error==null || error.isBlank())
			throw new IllegalArgumentException("Error cannot be null or blank");
		
		Path sourceVideoPath=entry.getVideoPath();
		Path targetVideoPath=spoolDirectories.getPending()
				.resolve(entry.getVideoPath().getFileName()).normalize();
		
		moveFile(sourceVideoPath, targetVideoPath);
		
		RecordingSpoolEntry newEntry=entry.recordUploadFailure(targetVideoPath, error);

		try
		{
			metadataStore.save(newEntry);
		}
		catch(RuntimeException metadataException)
		{
			try
			{
				moveFile(targetVideoPath, sourceVideoPath);
			}
			catch(RuntimeException rollbackException)
			{
				metadataException.addSuppressed(rollbackException);
			}
			
			throw metadataException;
		}
		metadataStore.delete(metadataStore.getMetadataPath(sourceVideoPath));
		
		return newEntry;
	}
	
	public synchronized RecordingSpoolEntry moveToFailed(RecordingSpoolEntry entry, String error)
	{
		if(entry==null)
			throw new IllegalArgumentException("RecordingSpoolEntry cannot be null");

		if(error==null || error.isBlank())
			throw new IllegalArgumentException("Error cannot be null or blank");
		
		Path sourceVideoPath=entry.getVideoPath();
		Path targetVideoPath=spoolDirectories.getFailed()
				.resolve(entry.getVideoPath().getFileName()).normalize();
		
		moveFile(sourceVideoPath, targetVideoPath);
		
		RecordingSpoolEntry newEntry=entry.recordPermanentUploadFailure(targetVideoPath, error);

		try
		{
			metadataStore.save(newEntry);
		}
		catch(RuntimeException metadataException)
		{
			try
			{
				moveFile(targetVideoPath, sourceVideoPath);
			}
			catch(RuntimeException rollbackException)
			{
				metadataException.addSuppressed(rollbackException);
			}
			
			throw metadataException;
		}
		metadataStore.delete(metadataStore.getMetadataPath(sourceVideoPath));
		
		return newEntry;
	}
	
	public synchronized RecordingSpoolEntry moveToUploaded(RecordingSpoolEntry entry)
	{
		if(entry==null)
			throw new IllegalArgumentException("RecordingSpoolEntry cannot be null");
		
		Path sourceVideoPath=entry.getVideoPath();
		Path targetVideoPath=spoolDirectories.getUploaded()
				.resolve(entry.getVideoPath().getFileName()).normalize();
		
		moveFile(sourceVideoPath, targetVideoPath);
		
		RecordingSpoolEntry newEntry=entry.markUploaded(targetVideoPath);

		try
		{
			metadataStore.save(newEntry);
		}
		catch(RuntimeException metadataException)
		{
			try
			{
				moveFile(targetVideoPath, sourceVideoPath);
			}
			catch(RuntimeException rollbackException)
			{
				metadataException.addSuppressed(rollbackException);
			}
			
			throw metadataException;
		}
		metadataStore.delete(metadataStore.getMetadataPath(sourceVideoPath));
		
		return newEntry;
	}
	
	public synchronized void deleteEntry(RecordingSpoolEntry entry)
	{
		if(entry==null)
			throw new IllegalArgumentException("RecordingSpoolEntry cannot be null");
		
		try {
			Files.deleteIfExists(entry.getVideoPath());
		} catch (IOException ex) {
			throw new RecordingSpoolException("Failed to delete RecordingSpoolEntry: "
					+entry.getVideoPath(), ex);
		}
		
		try {
			Files.deleteIfExists(metadataStore.getMetadataPath(entry.getVideoPath()));
		} catch (IOException ex) {
			throw new RecordingSpoolException("Failed to delete RecordingSpoolEntry: "
					+entry.getVideoPath(), ex);
		}
	}
	
	public synchronized RecordingSpoolEntry markUploadConfirmed(RecordingSpoolEntry entry)
	{
	    if(entry == null)
	        throw new IllegalArgumentException(
	                "RecordingSpoolEntry cannot be null"
	        );

	    RecordingSpoolEntry confirmedEntry =
	            entry.markUploadConfirmed();

	    metadataStore.save(confirmedEntry);

	    return confirmedEntry;
	}
	
	private void moveFile(Path source, Path target)
	{
		Path normalizedRoot =
		        spoolDirectories.getRoot()
		                .toAbsolutePath()
		                .normalize();

		Path normalizedSource =
		        source.toAbsolutePath().normalize();

		Path normalizedTarget =
		        target.toAbsolutePath().normalize();
		
		if(!normalizedSource.startsWith(normalizedRoot) || 
				!normalizedTarget.startsWith(normalizedRoot))
			throw new IllegalArgumentException("Source and Target must have the same root");
		
		try 
		{
			if(!Files.exists(normalizedSource))
					throw new IllegalArgumentException("Source file does not exist");
			
			if(Files.exists(normalizedTarget))
				throw new IllegalArgumentException("Target path already exists");
			
			try
			{
				Files.move(normalizedSource, normalizedTarget, StandardCopyOption.ATOMIC_MOVE);
			}
			catch(AtomicMoveNotSupportedException ex)
			{
				Files.move(normalizedSource, normalizedTarget);
			}
		} catch (IOException ex) {
			throw new RecordingSpoolException(
	                "Failed to move recording from "
	                        + normalizedSource
	                        + " to "
	                        + normalizedTarget,
	                ex
	        );
		}
	}
	
	public synchronized Optional<Path> quarantineActiveFile(Path videoPath)
	{
		if(videoPath==null)
			throw new IllegalArgumentException("videoPath cannot be null");
		
		if(!FileUtil.isDirectChildOf(videoPath, spoolDirectories.getActive()))
			throw new IllegalArgumentException("videoPath must be in active path");
		
		if(!FileUtil.getFileExtension(videoPath).equals("mp4"))
			throw new IllegalArgumentException("videoPath must be .mp4");
		
		Path normalizedPath=videoPath.toAbsolutePath().normalize();

		Path failedPath;
		try
		{
			BasicFileAttributes attributes=Files.readAttributes(
							normalizedPath, 
							BasicFileAttributes.class,
	                        LinkOption.NOFOLLOW_LINKS
							);
			
			if(!attributes.isRegularFile())
				throw new RecordingSpoolException("attributes must be regular file");
			
			Path filename=Paths.get("partial-"
					+UUID.randomUUID().toString()
					+"-"+normalizedPath.getFileName());
			
			failedPath=FileUtil.normalizeAbsolute(spoolDirectories.getFailed().resolve(filename));
		}
		catch(NoSuchFileException ex)
		{
			return Optional.empty();
		}
		catch(IOException ex)
		{
			throw new RecordingSpoolException("Failed to quarantine active file", ex);
		}
		
		moveFile(normalizedPath, failedPath);
		
		return Optional.of(failedPath);
	}
}