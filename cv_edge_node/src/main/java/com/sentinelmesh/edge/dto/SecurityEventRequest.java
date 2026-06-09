package com.sentinelmesh.edge.dto;

import java.time.Instant;
import java.util.Map;

import com.sentinelmesh.edge.events.SecurityEventSeverity;
import com.sentinelmesh.edge.events.SecurityEventType;

public class SecurityEventRequest 
{
	private final SecurityEventType eventType;
	private final SecurityEventSeverity severity;
	private final Double confidence;
	private final Instant occurredAt;
	private final String metadataJson;
	
	public SecurityEventRequest(
			SecurityEventType eventType,
			SecurityEventSeverity severity,
			Double confidence,
			Instant occurredAt,
			String metadataJson
			)
	{
		if(eventType==null)
			throw new IllegalArgumentException("EventType cannot be null");
		
		if(severity==null)
			throw new IllegalArgumentException("Severity cannot be null");
		
		if(confidence!=null && (confidence<0.0 || confidence>1.0))
			throw new IllegalArgumentException("Confidence must be between 0.0 and 1.0");
		
		if(occurredAt==null)
			throw new IllegalArgumentException("occurredAt cannot be null");
		
		if(metadataJson==null)
			throw new IllegalArgumentException("Metadata cannot be null");
		
		this.eventType=eventType;
		this.severity=severity;
		this.occurredAt=occurredAt;
		this.confidence=confidence;
		this.metadataJson=metadataJson;
	}
	
	public SecurityEventType getEventType()
	{
		return eventType;
	}
	
	public SecurityEventSeverity getSeverity()
	{
		return severity;
	}
	
	public Instant getOccurredAt()
	{
		return occurredAt;
	}
	
	public Double getConfidence()
	{
		return confidence;
	}
	
	public String getMetadataJson()
	{
		return metadataJson;
	}
}
