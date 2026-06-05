package com.sentinelmesh.edge.processing;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.sentinelmesh.edge.client.HeartbeatClient;
import com.sentinelmesh.edge.client.SentinelMeshApiClient;
import com.sentinelmesh.edge.config.EdgeNodeConfig;

public class HeartbeatLoop
{
	private final HeartbeatClient heartbeatClient;
	private final EdgeNodeConfig config;
	private ScheduledExecutorService scheduler;
	private volatile boolean running;
	private final int heartbeatIntervalSeconds;
	
	public HeartbeatLoop(SentinelMeshApiClient apiClient, EdgeNodeConfig config)
	{
		if(apiClient==null)
			throw new IllegalArgumentException("SentinelMeshApiClient cannot be null");
		
		if(config==null)
			throw new IllegalArgumentException("EdgeNodeConfig cannot be null");
		
		this.heartbeatClient=new HeartbeatClient(apiClient);
		this.config=config;
		
		running=false;
		heartbeatIntervalSeconds=config.getHeartbeatIntervalSeconds();
	}
	
	public void start()
	{
		if(running)
			return;
		
		scheduler=Executors.newScheduledThreadPool(1);
		
		running=true;
		
		Runnable heartbeat=() -> sendHeartbeatOnce();
		
		scheduler.scheduleAtFixedRate(heartbeat, 0, heartbeatIntervalSeconds, TimeUnit.SECONDS);
	}
	
	public void stop()
	{
		running=false;
		
		if(scheduler!=null)
		{
			scheduler.shutdown();
			scheduler=null;
		}
	}
	
	public void sendHeartbeatOnce()
	{
		try
		{
			heartbeatClient.sendHeartbeat(config.getDeviceId(), config.getApiKey());
			System.out.println("Heartbeat sent");
		} catch(Exception ex)
		{
			System.out.println("Heartbeat failed: "+ex);
		}
	}
	
	public boolean isRunning()
	{
		return running;
	}
}
