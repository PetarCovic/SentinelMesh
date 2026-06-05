package com.sentinelmesh.edge.client;

import java.util.UUID;

import com.sentinelmesh.edge.devices.DeviceStatus;
import com.sentinelmesh.edge.dto.HeartbeatRequest;

public class HeartbeatClient 
{
	private final SentinelMeshApiClient apiClient;
	
	public HeartbeatClient(SentinelMeshApiClient apiClient)
	{
		if(apiClient==null)
			throw new IllegalArgumentException("SentinelMeshApiClient cannot be null");
		
		this.apiClient=apiClient;
	}
	
	public String sendHeartbeat(UUID deviceId, String apiKey)
	{
		if(deviceId==null)
			throw new IllegalArgumentException("Device Id cannot be null");
			
		if(apiKey==null || apiKey.isBlank())
			throw new IllegalArgumentException("Api Key cannot be null or blank");
		
		apiKey=apiKey.trim();
		
		String path="/api/devices/"+deviceId+"/heartbeat";
		
		HeartbeatRequest request=new HeartbeatRequest(DeviceStatus.ONLINE);
		
		String post=apiClient.post(path, request, apiKey);
		
		return post;
	}
}