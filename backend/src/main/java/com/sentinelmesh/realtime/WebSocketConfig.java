package com.sentinelmesh.realtime;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer
{
	private final DashboardWebSocketHandler dashboardWebSocketHandler;
	
	public WebSocketConfig(DashboardWebSocketHandler dashboardWebSocketHandler)
	{
		this.dashboardWebSocketHandler=dashboardWebSocketHandler;
	}
	
	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry)
	{
		registry.addHandler(dashboardWebSocketHandler, "/ws/dashboard")
				.setAllowedOrigins("http://localhost:5173");
	}
}
