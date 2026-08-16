package com.sentinelmesh.recordings;

import java.time.Instant;
import java.util.UUID;

public class RecordingSegmentResponse 
{
	private final UUID id;
	private final UUID deviceId;
	private final UUID segmentId;
	private final String contentType;
	private final long fileSizeBytes;
	private final int durationSeconds;
	private final int width;
	private final int height;
	private final String videoUrl;
	private final Instant segmentStartTime;
	private final Instant segmentEndTime;
	private final Instant createdAt;
	
	public RecordingSegmentResponse(
			UUID id,
			UUID deviceId,
			UUID segmentId,
			String contentType,
			long fileSizeBytes,
			int durationSeconds,
			int width,
			int height,
			String videoUrl,
			Instant segmentStartTime,
			Instant segmentEndTime,
			Instant createdAt
			)
	{
		this.id=id;
		this.deviceId=deviceId;
		this.segmentId=segmentId;
		this.contentType=contentType;
		this.fileSizeBytes=fileSizeBytes;
		this.durationSeconds=durationSeconds;
		this.width=width;
		this.height=height;
		this.videoUrl=videoUrl;
		this.segmentStartTime=segmentStartTime;
		this.segmentEndTime=segmentEndTime;
		this.createdAt=createdAt;
	}
	
	public UUID getId()
	{
		return id;
	}
	
	public UUID getDeviceId()
	{
		return deviceId;
	}
	
	public UUID getSegmentId()
	{
		return segmentId;
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
	
	public Instant getSegmentStartTime()
	{
		return segmentStartTime;
	}
	
	public Instant getSegmentEndTime()
	{
		return segmentEndTime;
	}
	
	public Instant getCreatedAt()
	{
		return createdAt;
	}
}
