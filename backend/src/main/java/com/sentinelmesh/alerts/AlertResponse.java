package com.sentinelmesh.alerts;

import java.time.Instant;
import java.util.UUID;

public class AlertResponse 
{
	private UUID id;
	private UUID securityEventId;
	private UUID deviceId;
	private String deviceName;
	private AlertSeverity severity;
	private AlertStatus status;
	private String title;
	private String message;
	private Instant createdAt;
	private Instant acknowledgedAt;
	private Instant resolvedAt;
	
	public AlertResponse(
			UUID id,
			UUID securityEventId,
			UUID deviceId,
			String deviceName,
			AlertSeverity severity,
			AlertStatus status,
			String title,
			String message,
			Instant createdAt,
			Instant acknowledgedAt,
			Instant resolvedAt
			)
	{
		this.id=id;
		this.securityEventId=securityEventId;
		this.deviceId=deviceId;
		this.deviceName=deviceName;
		this.severity=severity;
		this.status=status;
		this.title=title;
		this.message=message;
		this.createdAt=createdAt;
		this.acknowledgedAt=acknowledgedAt;
		this.resolvedAt=resolvedAt;
	}
	
	public static AlertResponse from(Alert alert)
	{
		return new AlertResponse(
				alert.getId(),
				alert.getSecurityEvent().getId(),
				alert.getSecurityEvent().getDevice().getId(),
				alert.getSecurityEvent().getDevice().getName(),
				alert.getSeverity(),
				alert.getStatus(),
				alert.getTitle(),
				alert.getMessage(),
				alert.getCreatedAt(),
				alert.getAcknowledgedAt(),
				alert.getResolvedAt()
				);
	}
	
	public UUID getId()
	{
		return id;
	}
	
	public UUID getSecurityEventId()
	{
		return securityEventId;
	}
	
	public UUID getDeviceId()
	{
		return deviceId;
	}
	
	public String getDeviceName()
	{
		return deviceName;
	}
	
	public AlertSeverity getSeverity()
	{
		return severity;
	}
	
	public AlertStatus getStatus()
	{
		return status;
	}
	
	public String getTitle()
	{
		return title;
	}
	
	public String getMessage()
	{
		return message;
	}
	
	public Instant getCreatedAt()
	{
		return createdAt;
	}
	
	public Instant getAcknowledgedAt()
	{
		return acknowledgedAt;
	}
	
	public Instant getResolvedAt()
	{
		return resolvedAt;
	}
}
