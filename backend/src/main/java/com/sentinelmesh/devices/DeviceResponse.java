package com.sentinelmesh.devices;

import java.time.Instant;
import java.util.UUID;

public class DeviceResponse 
{
	private UUID id;
	private String name;
	private DeviceType type;
	private String location;
	private DeviceStatus status;
	private Instant lastSeenAt;
	private Instant createdAt;
	private Instant updatedAt;
	
	public DeviceResponse(
			UUID id,
			String name,
			DeviceType type,
			String location,
			DeviceStatus status,
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
		this.lastSeenAt=lastSeenAt;
		this.createdAt=createdAt;
		this.updatedAt=updatedAt;
	}
	
	public static DeviceResponse from(Device device)
	{
		return new DeviceResponse(
				device.getId(),
				device.getName(),
				device.getType(),
				device.getLocation(),
				device.getStatus(),
				device.getLastSeenAt(),
				device.getCreatedAt(),
				device.getUpdatedAt()
				);
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
}
