package com.sentinelmesh.edge.client;

import java.util.UUID;

public class LiveFrameUploadClient 
{
	private final SentinelMeshApiClient apiClient;
	
	public LiveFrameUploadClient(SentinelMeshApiClient apiClient)
	{
		if(apiClient==null)
			throw new IllegalArgumentException("ApiClient cannot be null");
		
		this.apiClient=apiClient;
	}
	
	public void uploadLiveFrame(UUID deviceId, String apiKey, byte[] imageBytes)
	{
		if(deviceId==null)
			throw new IllegalArgumentException("DeviceId cannot be null");
		
		if(apiKey==null || apiKey.isBlank())
			throw new IllegalArgumentException("ApiKey cannot be null or blank");
		
		if(imageBytes==null || imageBytes.length==0)
			throw new IllegalArgumentException("ImageBytes cannot be null or empty");
		
		String path="/api/devices/"+deviceId+"/live-frame";
		String fieldName="file";
		String filename="live-frame.jpg";
		String contentType="image/jpeg";
		
		apiClient.postMultipartFile(path, apiKey, fieldName, filename, contentType, imageBytes);
	}
}
