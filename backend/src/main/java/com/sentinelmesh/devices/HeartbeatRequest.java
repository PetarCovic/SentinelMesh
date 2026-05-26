package com.sentinelmesh.devices;

import jakarta.validation.constraints.NotNull;

public class HeartbeatRequest 
{
	@NotNull(message="Device status is required")
	private DeviceStatus status;
	
	public HeartbeatRequest()
	{
		
	}
	
	public DeviceStatus getStatus()
	{
		return status;
	}
	
	public void setStatus(DeviceStatus status)
	{
		this.status=status;
	}
}
