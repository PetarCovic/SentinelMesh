package com.sentinelmesh.devices;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name="devices")
public class Device 
{
	@Id
	@GeneratedValue(strategy=GenerationType.UUID)
	private UUID id;
	
	@Column(nullable=false, length=100)
	private String name;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable=false, length=50)
	private DeviceType type;
	
	@Column(length=100)
	private String location;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable=false, length=30)
	private DeviceStatus status;
	
	@Column(name="api_key_hash")
	private String apiKeyHash;
	
	@Column(name="last_seen_at")
	private Instant lastSeenAt;
	
	@Column(name="created_at", nullable=false)
	private Instant createdAt;
	
	@Column(name="updated_at", nullable=false)
	private Instant updatedAt;
	
	protected Device()
	{
		
	}
	
	public Device(String name, DeviceType type, String location)
	{
		this.name=name;
		this.type=type;
		this.location=location;
		this.status=DeviceStatus.OFFLINE;
		this.createdAt=Instant.now();
		this.updatedAt=Instant.now();
	}
	
	@PrePersist
	public void prePersist()
	{
		Instant now=Instant.now();
		
		if(createdAt==null)
		{
			createdAt=now;
		}
		
		if(updatedAt==null)
		{
			updatedAt=null;
		}
		
		if(status==null)
		{
			status=DeviceStatus.OFFLINE;
		}
	}
	
	@PreUpdate
	public void preUpdate()
	{
		updatedAt=Instant.now();
	}
	
	public UUID getId()
	{
		return id;
	}
	
	public String getName()
	{
		return name;
	}
	
	public DeviceType getType()
	{
		return type;
	}
	
	public String getLocation()
	{
		return location;
	}
	
	public DeviceStatus getStatus()
	{
		return status;
	}
	
	public String getApiKeyHash()
	{
		return apiKeyHash;
	}
	
	public Instant getLastSeenAt()
	{
		return lastSeenAt;
	}
	
	public Instant getCreatedAt()
	{
		return createdAt;
	}
	
	public Instant getUpdatedAt()
	{
		return updatedAt;
	}
	
	public void setName(String name)
	{
		this.name=name;
	}
	
	public void setType(DeviceType type)
	{
		this.type=type;
	}
	
	public void setLocation(String location)
	{
		this.location=location;
	}
	
	public void setStatus(DeviceStatus status)
	{
		this.status=status;
	}
	
	public void setApiKeyHash(String apiKeyHash)
	{
		this.apiKeyHash=apiKeyHash;
	}
	
	public void setLastSeenAt(Instant lastSeenAt)
	{
		this.lastSeenAt=lastSeenAt;
	}
}
