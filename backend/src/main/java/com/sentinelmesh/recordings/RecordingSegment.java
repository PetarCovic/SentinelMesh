package com.sentinelmesh.recordings;

import java.time.Instant;
import java.util.UUID;

import com.sentinelmesh.devices.Device;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name="recording_segments")
public class RecordingSegment 
{
	@Id
	@GeneratedValue(strategy=GenerationType.UUID)
	private UUID id; // Backend database primary key
	
	@Column(name = "segment_id", nullable = false, unique = true)
	private UUID segmentId; // Stable edge-generated recording identifier
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="device_id", nullable=false)
	private Device device;
	
	@Column(nullable=false, length=256)
	private String originalFilename;
	
	@Column(nullable=false, length=256)
	private String storedFilename;
	
	@Column(nullable=false, length=256)
	private String contentType;
	
	@Column(nullable=false)
	private long fileSizeBytes;
	
	@Column(nullable=false)
	private int durationSeconds;
	
	@Column(nullable=false)
	private int width;
	
	@Column(nullable=false)
	private int height;
	
	@Column(nullable=false, length=1024)
	private String storagePath;
	
	@Column(name="segment_start_time", nullable=false)
	private Instant segmentStartTime;
	
	@Column(name="segment_end_time", nullable=false)
	private Instant segmentEndTime;
	
	@Column(name="created_at", nullable=false)
	private Instant createdAt;
	
	protected RecordingSegment()
	{
		
	}
	
	public RecordingSegment(
			UUID segmentId,
			Device device,
			String originalFilename,
			String storedFilename,
			String contentType,
			long fileSizeBytes,
			int durationSeconds,
			int width,
			int height,
			String storagePath,
			Instant segmentStartTime,
			Instant segmentEndTime
			)
	{
		this.segmentId=segmentId;
		this.device=device;
		this.originalFilename=originalFilename;
		this.storedFilename=storedFilename;
		this.contentType=contentType;
		this.fileSizeBytes=fileSizeBytes;
		this.durationSeconds=durationSeconds;
		this.width=width;
		this.height=height;
		this.storagePath=storagePath;
		this.segmentStartTime=segmentStartTime;
		this.segmentEndTime=segmentEndTime;
		this.createdAt=Instant.now();
	}
	
	public UUID getId()
	{
		return id;
	}
	
	public UUID getSegmentId()
	{
		return segmentId;
	}
	
	public Device getDevice()
	{
		return device;
	}
	
	public String getOriginalFilename()
	{
		return originalFilename;
	}
	
	public String getStoredFilename()
	{
		return storedFilename;
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
	
	public String getStoragePath()
	{
		return storagePath;
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
	
	public void setDevice(Device device)
	{
		this.device=device;
	}
	
	public void setOriginalFilename(String originalFilename)
	{
		this.originalFilename=originalFilename;
	}
	
	public void setStoredFilename(String storedFilename)
	{
		this.storedFilename=storedFilename;
	}
	
	public void setContentType(String contentType)
	{
		this.contentType=contentType;
	}
	
	public void setFileSizeBytes(long fileSizeBytes)
	{
		this.fileSizeBytes=fileSizeBytes;
	}
	
	public void setDurationSeconds(int durationSeconds)
	{
		this.durationSeconds=durationSeconds;
	}
	
	public void setWidth(int width)
	{
		this.width=width;
	}
	
	public void setHeight(int height)
	{
		this.height=height;
	}
	
	public void setStoragePath(String storagePath)
	{
		this.storagePath=storagePath;
	}
	
	public void setSegmentStartTime(Instant segmentStartTime)
	{
		this.segmentStartTime=segmentStartTime;
	}
	
	public void setSegmentEndTime(Instant segmentEndTime)
	{
		this.segmentEndTime=segmentEndTime;
	}
}