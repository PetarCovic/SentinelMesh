package com.sentinelmesh.edge.client;

import java.util.UUID;

import com.sentinelmesh.edge.detection.DetectionResult;
import com.sentinelmesh.edge.detection.DetectionType;
import com.sentinelmesh.edge.dto.SecurityEventRequest;
import com.sentinelmesh.edge.events.SecurityEventSeverity;
import com.sentinelmesh.edge.events.SecurityEventType;

public class SecurityEventClient 
{
	private final SentinelMeshApiClient apiClient;
	
	public SecurityEventClient(SentinelMeshApiClient apiClient)
	{
		if(apiClient == null)
			throw new IllegalArgumentException("SentinelMeshApiClient cannot be null");
		
		this.apiClient = apiClient;
	}
	
	public String sendEvent(UUID deviceId, String apiKey, DetectionResult detection)
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
		
		return apiClient.post(path, request, apiKey);
	}
	
	private SecurityEventType mapEventType(DetectionType detectionType)
	{
		if(detectionType == null)
			throw new IllegalArgumentException("DetectionType cannot be null");
		
		if(detectionType == DetectionType.MOTION_DETECTED)
			return SecurityEventType.MOTION_DETECTED;
		
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
		StringBuilder sb = new StringBuilder();
		
		sb.append("{");
		sb.append("\"deviceId\":\"").append(deviceId).append("\",");
		sb.append("\"detectionType\":\"").append(detection.getType()).append("\",");
		sb.append("\"boundingBox\":{");
		sb.append("\"x\":").append(detection.getBoundingBoxX()).append(",");
		sb.append("\"y\":").append(detection.getBoundingBoxY()).append(",");
		sb.append("\"width\":").append(detection.getBoundingBoxWidth()).append(",");
		sb.append("\"height\":").append(detection.getBoundingBoxHeight());
		sb.append("},");
		sb.append("\"metadata\":\"").append(escapeJson(detection.getMetadata().toString())).append("\"");
		sb.append("}");
		
		return sb.toString();
	}
	
	private String escapeJson(String value)
	{
		if(value == null)
			return "";
		
		return value
				.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\n", "\\n")
				.replace("\r", "\\r");
	}
}