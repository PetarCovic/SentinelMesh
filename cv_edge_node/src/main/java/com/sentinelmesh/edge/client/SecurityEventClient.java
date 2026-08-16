package com.sentinelmesh.edge.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinelmesh.edge.detection.DetectionResult;
import com.sentinelmesh.edge.detection.DetectionType;
import com.sentinelmesh.edge.dto.SecurityEventRequest;
import com.sentinelmesh.edge.dto.SecurityEventResponse;
import com.sentinelmesh.edge.events.SecurityEventSeverity;
import com.sentinelmesh.edge.events.SecurityEventType;

public class SecurityEventClient 
{
	private final SentinelMeshApiClient apiClient;
	private final ObjectMapper objectMapper;
	
	public SecurityEventClient(SentinelMeshApiClient apiClient)
	{
		if(apiClient == null)
			throw new IllegalArgumentException("SentinelMeshApiClient cannot be null");
		
		this.apiClient = apiClient;
		this.objectMapper=new ObjectMapper();
	}
	
	public UUID sendEvent(UUID deviceId, String apiKey, DetectionResult detection)
	{
		if(deviceId == null)
			throw new IllegalArgumentException("DeviceId cannot be null");
		
		if(apiKey == null || apiKey.isBlank())
			throw new IllegalArgumentException("ApiKey cannot be null or blank");
		
		if(detection == null)
			throw new IllegalArgumentException("DetectionResult cannot be null");
		
		String path = "/api/devices/" + deviceId + "/events";
		
		SecurityEventRequest request = new SecurityEventRequest(
				mapEventType(detection.getType()),
				mapSeverity(detection),
				detection.getConfidence(),
				detection.getTimestamp(),
				buildMetadataJson(deviceId, detection)
		);
		
		String responseJson=apiClient.post(path, request, apiKey);
		
		SecurityEventResponse response;
		response = apiClient.parseResponse(responseJson, SecurityEventResponse.class);
		
		if(response==null)
		    throw new IllegalStateException("Security event response was empty");

		if(response.getId()==null)
		    throw new IllegalStateException("Security event response did not include an id");
		
		return response.getId();
	}
	
	private SecurityEventType mapEventType(DetectionType detectionType)
	{
		if(detectionType == null)
			throw new IllegalArgumentException("DetectionType cannot be null");
		
		if(detectionType == DetectionType.MOTION_DETECTED)
			return SecurityEventType.MOTION_DETECTED;
		
		if(detectionType==DetectionType.PERSON_DETECTED)
			return SecurityEventType.PERSON_DETECTED;
		
		throw new IllegalArgumentException("Unsupported detection type: " + detectionType);
	}
	
	private SecurityEventSeverity mapSeverity(DetectionResult detection)
	{
		double confidence = detection.getConfidence();
		
		if(confidence >= 0.85)
			return SecurityEventSeverity.HIGH;
		
		if(confidence >= 0.50)
			return SecurityEventSeverity.MEDIUM;
		
		return SecurityEventSeverity.LOW;
	}
	
	private String buildMetadataJson(UUID deviceId, DetectionResult detection)
	{
		try
		{
			Map<String, Object> boundingBox = new HashMap<>();
			boundingBox.put("x", detection.getBoundingBoxX());
			boundingBox.put("y", detection.getBoundingBoxY());
			boundingBox.put("width", detection.getBoundingBoxWidth());
			boundingBox.put("height", detection.getBoundingBoxHeight());
			
			Map<String, Object> metadata = new HashMap<>();
			metadata.put("deviceId", deviceId.toString());
			metadata.put("detectionType", detection.getType().toString());
			metadata.put("boundingBox", boundingBox);
			metadata.put("frameMetadata", detection.getMetadata());
			
			return objectMapper.writeValueAsString(metadata);
		}
		catch (Exception ex)
		{
			throw new IllegalStateException("Failed to build metadata JSON", ex);
		}
	}
}