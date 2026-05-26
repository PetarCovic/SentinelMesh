package com.sentinelmesh.events;

import java.time.Instant;
import java.util.UUID;

import com.sentinelmesh.devices.Device;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name="security_events")
public class SecurityEvent 
{
	@Id
	@GeneratedValue(strategy=GenerationType.UUID)
	private UUID id;
	
	@ManyToOne(fetch=FetchType.LAZY, optional=false)
	@JoinColumn(name="device_id", nullable=false)
	private Device device;
	
	@Enumerated(EnumType.STRING)
	@Column(name="event_type", nullable=false, length=50)
	private SecurityEventType eventType;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable=false, length=30)
	private SecurityEventSeverity severity;
	
	@Column
	private Double confidence;
	
	@Column(name="occurred_at", nullable=false)
	private Instant occurredAt;
	
	@Column(name="received_at", nullable=false)
	private Instant receivedAt;
	
	@Column(name="metadata_json", columnDefinition="TEXT")
	private String metadataJson;
	
	protected SecurityEvent()
	{
		//Required by JPA
	}
	
	public SecurityEvent(
			Device device,
			SecurityEventType eventType,
			SecurityEventSeverity severity,
			Double confidence,
			Instant occurredAt,
			String metadataJson
			)
	{
		this.device=device;
		this.eventType=eventType;
		this.severity=severity;
		this.confidence=confidence;
		this.occurredAt=occurredAt;
		this.metadataJson=metadataJson;
		this.receivedAt=Instant.now();
	}
	
	@PrePersist
	public void prePersist()
	{
		if(receivedAt==null)
			receivedAt=Instant.now();
		
		if(occurredAt==null)
			occurredAt=Instant.now();
		
		if(severity==null)
			severity=SecurityEventSeverity.LOW;
	}
	
	public UUID getId()
	{
		return id;
	}
	
	public Device getDevice()
	{
		return device;
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
