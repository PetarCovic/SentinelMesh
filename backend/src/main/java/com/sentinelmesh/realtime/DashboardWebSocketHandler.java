package com.sentinelmesh.realtime;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class DashboardWebSocketHandler extends TextWebSocketHandler
{
	private final Set<WebSocketSession> sessions=ConcurrentHashMap.newKeySet();
	
	@Override
	public void afterConnectionEstablished(WebSocketSession session)
	{
		sessions.add(session);
		System.out.println("Dashboard WebSocket connected: "+session.getId());
	}
	
	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status)
	{
		sessions.remove(session);
		System.out.println("Dashboard WebSocket disconnected: "+session.getId());
	}
	
	public void broadcast(String message)
	{
		for(WebSocketSession session : sessions)
		{
			if(!session.isOpen())
			{
				sessions.remove(session);
				continue;
			}
			
			try
			{
				session.sendMessage(new TextMessage(message));
			}catch(IOException ex)
			{
				sessions.remove(session);
				System.err.println("Failed to send WebSocket message: "+ex.getMessage());
			}
		}
	}
	
	public int getActiveSessionCount()
	{
		return sessions.size();
	}
}
