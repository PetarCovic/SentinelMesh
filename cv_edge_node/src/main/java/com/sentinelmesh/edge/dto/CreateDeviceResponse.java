package com.sentinelmesh.edge.dto;

import java.time.Instant;
import java.util.UUID;
import com.sentinelmesh.edge.devices.DeviceStatus;
import com.sentinelmesh.edge.devices.DeviceType;

public class CreateDeviceResponse 
{
	private UUID id;
	private String name;
	private DeviceType type;
	private String location;
	private DeviceStatus status;
	private String apiKey;
	private Instant lastSeenAt;
	private Instant createdAt;
	private Instant updatedAt;
	
	public CreateDeviceResponse()
	{
		
	}
	
	public CreateDeviceResponse(
			UUID id,
			String name,
			DeviceType type,
			String location,
			DeviceStatus status,
			String apiKey,
			Instant lastSeenAt,
			Instant createdAt,
			Instant updatedAt
			)
	{
		this.id=id;
		this.name=name;
		this.type=type;
		this.location=location;
		this.status=status;
		this.apiKey=apiKey;
		this.lastSeenAt=lastSeenAt;
		this.createdAt=createdAt;
		this.updatedAt=updatedAt;
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
	
	public String getApiKey()
	{
		return apiKey;
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
	
	public void setId(UUID id)
	{
		this.id=id;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}

	public void setType(DeviceType type)
	{
		this.type = type;
	}

	public void setLocation(String location)
	{
		this.location = location;
	}

	public void setStatus(DeviceStatus status)
	{
		this.status = status;
	}

	public void setApiKey(String apiKey)
	{
		this.apiKey = apiKey;
	}

	public void setLastSeenAt(Instant lastSeenAt)
	{
		this.lastSeenAt = lastSeenAt;
	}

	public void setCreatedAt(Instant createdAt)
	{
		this.createdAt = createdAt;
	}

	public void setUpdatedAt(Instant updatedAt)
	{
		this.updatedAt = updatedAt;
	}
	
	@Override
	public String toString()
	{
		return "CreateDeviceResponse{" +
				"id=" + id +
				", name='" + name + '\'' +
				", type=" + type +
				", location='" + location + '\'' +
				", status=" + status +
				", apiKey='[REDACTED]'" +
				", lastSeenAt=" + lastSeenAt +
				", createdAt=" + createdAt +
				", updatedAt=" + updatedAt +
				'}';
	}
}
