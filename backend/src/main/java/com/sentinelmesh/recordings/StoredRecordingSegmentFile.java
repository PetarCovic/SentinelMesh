package com.sentinelmesh.recordings;

import java.nio.file.Path;

public class StoredRecordingSegmentFile 
{
	private final String storedFilename;
	private final Path storagePath;
	private final String contentType;
	private final long fileSizeBytes;
	private final int durationSeconds;
	private final int width;
	private final int height;
	
	public StoredRecordingSegmentFile(
			String storedFilename,
			Path storagePath,
			String contentType,
			long fileSizeBytes,
			int durationSeconds,
			int width,
			int height
			)
	{
		this.storedFilename=storedFilename;
		this.storagePath=storagePath;
		this.contentType=contentType;
		this.fileSizeBytes=fileSizeBytes;
		this.durationSeconds=durationSeconds;
		this.width=width;
		this.height=height;
	}
	
	public String getStoredFilename()
	{
		return storedFilename;
	}
	
	public Path getStoragePath()
	{
		return storagePath;
	}
	
	public String getContentType()
	{
		return contentType;
	}
	
	public long getFileSizeBytes()
	{
		return fileSizeBytes;
	}
	
	public int getDurationSeconds()
	{
		return durationSeconds;
	}
	
	public int getWidth()
	{
		return width;
	}
	
	public int getHeight()
	{
		return height;
	}
}
