package com.sentinelmesh.edge.recording;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.sentinelmesh.edge.exceptions.RecordingSpoolException;

public class RecordingSpoolDirectories 
{
	private final Path root;
	private final Path active;
	private final Path pending;
	private final Path uploading;
	private final Path uploaded;
	private final Path failed;
	
	public RecordingSpoolDirectories(Path root)
	{
		if(root==null || root.toString().isBlank())
			throw new IllegalArgumentException("Root cannot be null or blank");
		
		try {
			this.root=root.toAbsolutePath().normalize();
			Files.createDirectories(this.root);
			this.active=Files.createDirectories(this.root.resolve("active"));
			this.pending=Files.createDirectories(this.root.resolve("pending"));
			this.uploading=Files.createDirectories(this.root.resolve("uploading"));
			this.uploaded=Files.createDirectories(this.root.resolve("uploaded"));
			this.failed=Files.createDirectories(this.root.resolve("failed"));
			
			validateDirectory(this.root, "root");
			validateDirectory(this.active, "active");
			validateDirectory(this.pending, "pending");
			validateDirectory(this.uploading, "uploading");
			validateDirectory(this.uploaded, "uploaded");
			validateDirectory(this.failed, "failed");
		} catch (IOException e) {
			throw new RecordingSpoolException("Cannot create recording spool directories", e);
		}
	}
	
	public Path getRoot() 
	{
		return root;
	}

	public Path getActive() 
	{
		return active;
	}

	public Path getPending()
{
		return pending;
	}

	public Path getUploading() 
	{
		return uploading;
	}

	public Path getUploaded() 
	{
		return uploaded;
	}

	public Path getFailed() 
	{
		return failed;
	}
	
	private void validateDirectory(Path path, String name)
	{
	    if(!Files.isDirectory(path))
	    {
	        throw new RecordingSpoolException(
	                name + " path is not a directory: " + path
	        );
	    }

	    if(!Files.isWritable(path))
	    {
	        throw new RecordingSpoolException(
	                name + " directory is not writable: " + path
	        );
	    }
	}
}