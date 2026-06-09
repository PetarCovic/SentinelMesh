package com.sentinelmesh.edge.dto;

import com.sentinelmesh.edge.devices.DeviceType;

public class CreateDeviceRequest 
{
	private final String name;
	private final DeviceType type;
	private final String location;
	
	public CreateDeviceRequest(
			String name,
			DeviceType type,
			String location
			)
	{
		if(name==null || name.isBlank())
			throw new IllegalArgumentException("Name cannot be null or blank");
		
		if(type==null)
			throw new IllegalArgumentException("DeviceType cannot be null");
		
		this.name=name;
		this.type=type;
		this.location=location;
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
}
