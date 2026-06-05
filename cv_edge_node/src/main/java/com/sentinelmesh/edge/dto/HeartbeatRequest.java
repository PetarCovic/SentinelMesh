package com.sentinelmesh.edge.dto;

import com.sentinelmesh.edge.devices.DeviceStatus;

public class HeartbeatRequest 
{
	private DeviceStatus status;
	
	public HeartbeatRequest()
	{
		
	}
	
	public HeartbeatRequest(DeviceStatus status)
	{
		this.status=status;
	}
	
	public DeviceStatus getStatus()
	{
		return status;
	}
	
	public void setStatus(DeviceStatus status)
	{
		if(status==null)
			throw new IllegalArgumentException("Status cannot be null");
		
		this.status=status;
	}
}
