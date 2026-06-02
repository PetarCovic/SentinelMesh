package com.sentinelmesh.realtime;

import java.util.Map;

import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

@Component
public class DashboardEventBroadcaster 
{
	private final DashboardWebSocketHandler dashboardWebSocketHandler;
	private final ObjectMapper objectMapper;
	
	public DashboardEventBroadcaster(
			DashboardWebSocketHandler dashboardWebSocketHandler, 
			ObjectMapper objectMapper
			)
	{
		this.dashboardWebSocketHandler=dashboardWebSocketHandler;
		this.objectMapper=objectMapper;
	}
	
	public void broadcast(DashboardEventType type, Map<String, Object> payload)
	{
		DashboardEventMessage message=DashboardEventMessage.of(type, payload);
		
		try
		{
			String json=objectMapper.writeValueAsString(message);
			dashboardWebSocketHandler.broadcast(json);
		}catch(Exception ex)
		{
			System.err.println("Failed to serialize dashboard event: "+ex.getMessage());
		}
	}
	
	public int getActiveSessionCount()
	{
		return dashboardWebSocketHandler.getActiveSessionCount();
	}
}
