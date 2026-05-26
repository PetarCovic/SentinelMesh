package com.sentinelmesh.devices;

import java.time.Instant;
import java.util.UUID;

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
	
	public static CreateDeviceResponse from(Device device, String rawApiKey)
	{
		return new CreateDeviceResponse(
				device.getId(),
				device.getName(),
				device.getType(),
				device.getLocation(),
				device.getStatus(),
				rawApiKey,
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
}