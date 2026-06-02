package com.sentinelmesh.realtime;

import java.time.Instant;
import java.util.Map;

public class DashboardEventMessage 
{
	private DashboardEventType type;
	private Instant timestamp;
	private Map<String, Object> payload;
	
	public DashboardEventMessage()
	{
		
	}
	
	public DashboardEventMessage(
			DashboardEventType type,
			Instant timestamp,
			Map<String, Object> payload
			)
	{
		this.type=type;
		this.timestamp=timestamp;
		this.payload=payload;
	}
	
	public static DashboardEventMessage of(
			DashboardEventType type,
			Map<String, Object> payload
			)
	{
		return new DashboardEventMessage(type, Instant.now(), payload);
	}
	
	public DashboardEventType getType()
	{
		return type;
	}
	
	public Instant getTimestamp()
	{
		return timestamp;
	}
	
	public Map<String, Object> getPayload()
	{
		return payload;
	}
	
	public void setType(DashboardEventType type)
	{
		this.type=type;
	}
	
	public void setPayload(Map<String, Object> payload)
	{
		this.payload=payload;
	}
}
