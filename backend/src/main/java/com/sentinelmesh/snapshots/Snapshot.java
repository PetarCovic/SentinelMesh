package com.sentinelmesh.snapshots;

import java.time.Instant;
import java.util.UUID;

import com.sentinelmesh.devices.Device;
import com.sentinelmesh.events.SecurityEvent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name="snapshots")
public class Snapshot 
{
	@Id
	@GeneratedValue(strategy=GenerationType.UUID)
	private UUID id;
	
	@ManyToOne
	@JoinColumn(name="event_id", unique=true, nullable=false)
	private SecurityEvent event;
	
	@ManyToOne
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
	private int width;
	
	@Column(nullable=false)
	private int height;
	
	@Column(nullable=false, length=1024)
	private String storagePath;
	
	@Column(name="created_at", nullable=false)
	private Instant createdAt;
	
	protected Snapshot()
	{
		
	}
	
	public Snapshot(
			SecurityEvent event,
			Device device,
			String originalFileName,
			String storedFileName,
			String contentType,
			long fileSizeBytes,
			int width,
			int height,
			String storagePath
			)
	{
		this.event=event;
		this.device=device;
		this.originalFilename=originalFileName;
		this.storedFilename=storedFileName;
		this.contentType=contentType;
		this.fileSizeBytes=fileSizeBytes;
		this.width=width;
		this.height=height;
		this.storagePath=storagePath;
		this.createdAt=Instant.now();
	}
	
	public UUID getId()
	{
		return id;
	}
	
	public SecurityEvent getEvent()
	{
		return event;
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
	
	public Instant getCreatedAt()
	{
		return createdAt;
	}
	
	public void setEvent(SecurityEvent event)
	{
		this.event=event;
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
}
