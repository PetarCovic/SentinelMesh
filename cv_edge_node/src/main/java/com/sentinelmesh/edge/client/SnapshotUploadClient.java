package com.sentinelmesh.edge.client;

import java.util.UUID;

public class SnapshotUploadClient 
{
	private final SentinelMeshApiClient apiClient;
	
	public SnapshotUploadClient(SentinelMeshApiClient apiClient)
	{
		if(apiClient==null)
			throw new IllegalArgumentException("ApiClient cannot be null");
		
		this.apiClient=apiClient;
	}
	
	public void uploadSnapshot(UUID deviceId, UUID eventId, String apiKey, byte[] imageBytes)
	{
		if(deviceId==null)
			throw new IllegalArgumentException("DeviceId cannot be null");
		
		if(eventId==null)
			throw new IllegalArgumentException("EventId cannot be null");
		
		if(apiKey==null || apiKey.isBlank())
			throw new IllegalArgumentException("ApiKey cannot be null or blank");
		
		if(imageBytes==null || imageBytes.length==0)
			throw new IllegalArgumentException("ImageBytes cannot be null or empty");
		
		String path="/api/devices/"+deviceId+"/events/"+eventId+"/snapshot";
		String fieldName="file";
		String filename="snapshot.jpg";
		String contentType="image/jpeg";
		
		apiClient.postMultipartFile(path, apiKey, fieldName, filename, contentType, imageBytes);
	}
}
