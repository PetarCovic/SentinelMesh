package com.sentinelmesh.snapshots;

import java.time.Instant;
import java.util.UUID;

public class SnapshotResponse 
{
	private final UUID id;
	private final UUID eventId;
	private final UUID deviceId;
	private final String contentType;
	private final long fileSizeBytes;
	private final int width;
	private final int height;
	private final String imageUrl;
	private final Instant createdAt;
	
	public SnapshotResponse(
			UUID id,
			UUID eventId,
			UUID deviceId,
			String contentType,
			long fileSizeBytes,
			int width,
			int height,
			String imageUrl,
			Instant createdAt
			)
	{
		this.id=id;
		this.eventId=eventId;
		this.deviceId=deviceId;
		this.contentType=contentType;
		this.fileSizeBytes=fileSizeBytes;
		this.width=width;
		this.height=height;
		this.imageUrl=imageUrl;
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
	
	public int getWidth()
	{
		return width;
	}
	
	public int getHeight()
	{
		return height;
	}
	
	public String getImageUrl()
	{
		return imageUrl;
	}
	
	public Instant getCreatedAt()
	{
		return createdAt;
	}
}