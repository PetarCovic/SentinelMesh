package com.sentinelmesh.devices.requests;

import com.sentinelmesh.devices.DeviceType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateDeviceRequest 
{
	@Size(max=100, message="Device name must be at most 100 characters")
	private String name;
	
	private DeviceType type;
	
	@Size(max=100, message="Location must be at most 100 characters")
	private String location;
	
	public UpdateDeviceRequest()
	{
		
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
}
