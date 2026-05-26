package com.sentinelmesh.events;

import java.time.Instant;
import java.util.UUID;

public class SecurityEventResponse 
{
	private UUID id;
	private UUID deviceId;
	private String deviceName;
	private SecurityEventType eventType;
	private SecurityEventSeverity severity;
	private Double confidence;
	private Instant occurredAt;
	private Instant receivedAt;
	private String metadataJson;
	
	public SecurityEventResponse(
			UUID id,
			UUID deviceId,
			String deviceName,
			SecurityEventType eventType,
			SecurityEventSeverity severity,
			Double confidence,
			Instant occurredAt,
			Instant receivedAt,
			String metadataJson
			)
	{
		this.id=id;
		this.deviceId=deviceId;
		this.deviceName=deviceName;
		this.eventType=eventType;
		this.severity=severity;
		this.confidence=confidence;
		this.occurredAt=occurredAt;
		this.receivedAt=receivedAt;
		this.metadataJson=metadataJson;
	}
	
	public static SecurityEventResponse from(SecurityEvent event)
	{
		return new SecurityEventResponse(
				event.getId(),
				event.getDevice().getId(),
				event.getDevice().getName(),
				event.getEventType(),
				event.getSeverity(),
				event.getConfidence(),
				event.getOccurredAt(),
				event.getReceivedAt(),
				event.getMetadataJson()
				);
	}
	
	public UUID getId()
	{
		return id;
	}
	
	public UUID getDeviceId()
	{
		return deviceId;
	}
	
	public String getDeviceName()
	{
		return deviceName;
	}
	
	public SecurityEventType getEventType()
	{
		return eventType;
	}
	
	public SecurityEventSeverity getSeverity()
	{
		return severity;
	}
	
	public Double getConfidence()
	{
		return confidence;
	}
	
	public Instant getOccurredAt()
	{
		return occurredAt;
	}
	
	public Instant getReceivedAt()
	{
		return receivedAt;
	}
	
	public String getMetadataJson()
	{
		return metadataJson;
	}
}