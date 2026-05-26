package com.sentinelmesh.events;

import java.time.Instant;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateSecurityEventRequest 
{
	@NotNull(message = "Event type is required")
	private SecurityEventType eventType;

	@NotNull(message = "Severity is required")
	private SecurityEventSeverity severity;

	@DecimalMin(value = "0.0", message = "Confidence must be at least 0.0")
	@DecimalMax(value = "1.0", message = "Confidence must be at most 1.0")
	private Double confidence;

	private Instant occurredAt;

	@Size(max = 5000, message = "Metadata JSON must be at most 5000 characters")
	private String metadataJson;
	
	public CreateSecurityEventRequest()
	{
		
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
	
	public String getMetadataJson()
	{
		return metadataJson;
	}
	
	public void setEventType(SecurityEventType eventType)
	{
		this.eventType=eventType;
	}
	
	public void setSeverity(SecurityEventSeverity severity)
	{
		this.severity=severity;
	}
	
	public void setConfidence(Double confidence)
	{
		this.confidence=confidence;
	}
	
	public void setOccurredAt(Instant occurredAt)
	{
		this.occurredAt=occurredAt;
	}
	
	public void setMetadataJson(String metadataJson)
	{
		this.metadataJson=metadataJson;
	}
}
