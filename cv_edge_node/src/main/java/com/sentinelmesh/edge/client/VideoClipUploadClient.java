package com.sentinelmesh.edge.client;

import java.util.UUID;

public class VideoClipUploadClient 
{
	private final SentinelMeshApiClient apiClient;
	
	public VideoClipUploadClient(SentinelMeshApiClient apiClient)
	{
		if(apiClient==null)
			throw new IllegalArgumentException("ApiClient cannot be null");
		
		this.apiClient=apiClient;
	}
	
	public void uploadVideoClip(UUID deviceId, UUID eventId, String apiKey, byte[] videoBytes)
	{
		if(deviceId==null)
			throw new IllegalArgumentException("DeviceId cannot be null");
		
		if(eventId==null)
			throw new IllegalArgumentException("EventId cannot be null");
		
		if(apiKey==null || apiKey.isBlank())
			throw new IllegalArgumentException("ApiKey cannot be null or blank");
		
		if(videoBytes==null || videoBytes.length==0)
			throw new IllegalArgumentException("VideoBytes cannot be null or empty");
		
		String path="/api/devices/"+deviceId+"/events/"+eventId+"/clip";
		String fieldName="file";
		String filename="event-clip.mp4";
		String contentType="video/mp4";
		
		apiClient.postMultipartFile(path, apiKey, fieldName, filename, contentType, videoBytes);
	}
}
