package com.sentinelmesh.clips;

import java.time.Instant;
import java.util.UUID;

public class VideoClipResponse 
{
	private final UUID id;
	private final UUID eventId;
	private final UUID deviceId;
	private final String contentType;
	private final long fileSizeBytes;
	private final int durationSeconds;
	private final int width;
	private final int height;
	private final String videoUrl;
	private final Instant createdAt;
	
	public VideoClipResponse(
			UUID id,
			UUID eventId,
			UUID deviceId,
			String contentType,
			long fileSizeBytes,
			int durationSeconds,
			int width,
			int height,
			String videoUrl,
			Instant createdAt
			)
	{
		this.id=id;
		this.eventId=eventId;
		this.deviceId=deviceId;
		this.contentType=contentType;
		this.fileSizeBytes=fileSizeBytes;
		this.durationSeconds=durationSeconds;
		this.width=width;
		this.height=height;
		this.videoUrl=videoUrl;
		this.createdAt=createdAt;
	}
	
	public UUID getId()
	{
		return id;
	}
	
	public UUID getEventId()
	{
		return eventId;
	}
	
	public UUID getDeviceId()
	{
		return deviceId;
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
	
	public String getVideoUrl()
	{
		return videoUrl;
	}
	
	public Instant getCreatedAt()
	{
		return createdAt;
	}
}