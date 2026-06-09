package com.sentinelmesh.edge.client;

import com.sentinelmesh.edge.devices.DeviceType;
import com.sentinelmesh.edge.dto.CreateDeviceRequest;
import com.sentinelmesh.edge.dto.CreateDeviceResponse;

public class DeviceRegistrationClient 
{
	private final SentinelMeshApiClient apiClient;
	
	public DeviceRegistrationClient(SentinelMeshApiClient apiClient)
	{
		if(apiClient==null)
			throw new IllegalArgumentException("SentinelMeshApiClient cannot be null");
		
		this.apiClient=apiClient;
	}
	
	public CreateDeviceResponse registerDevice(CreateDeviceRequest request)
	{
		if(request==null)
			throw new IllegalArgumentException("CreateDeviceRequest cannot be null");
		
		String path="/api/devices";
		String responseJson=apiClient.post(path, request);
		CreateDeviceResponse response=apiClient.parseResponse(responseJson, CreateDeviceResponse.class);
		
		if(response==null)
			throw new IllegalStateException("Device registration generated null response");
		if(response.getId()==null)
			throw new IllegalStateException("Response did not generate Id");
		
		if(response.getApiKey()==null || response.getApiKey().isBlank())
			throw new IllegalStateException("Response did not generate api key");
		
		return response;
	}
}