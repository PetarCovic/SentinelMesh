package com.sentinelmesh.rules;

import java.time.Instant;
import java.util.UUID;

import com.sentinelmesh.alerts.AlertSeverity;
import com.sentinelmesh.devices.DeviceType;
import com.sentinelmesh.events.SecurityEventSeverity;
import com.sentinelmesh.events.SecurityEventType;

public class AlertRuleResponse 
{
	private UUID id;
	private String name;
	private String description;
	private boolean enabled;
	private SecurityEventType eventType;
	private SecurityEventSeverity minimumSeverity;
	private DeviceType deviceType;
	private AlertSeverity alertSeverity;
	private String alertTitle;
	private String alertMessage;
	private Instant createdAt;
	private Instant updatedAt;
	
	public AlertRuleResponse(
			UUID id,
			String name,
			String description,
			boolean enabled,
			SecurityEventType eventType,
			SecurityEventSeverity minimumSeverity,
			DeviceType deviceType,
			AlertSeverity alertSeverity,
			String alertTitle,
			String alertMessage,
			Instant createdAt,
			Instant updatedAt
			)
	{
		this.id=id;
		this.name=name;
		this.description=description;
		this.enabled=enabled;
		this.eventType=eventType;
		this.minimumSeverity=minimumSeverity;
		this.deviceType=deviceType;
		this.alertSeverity=alertSeverity;
		this.alertTitle=alertTitle;
		this.alertMessage=alertMessage;
		this.createdAt=createdAt;
		this.updatedAt=updatedAt;
	}
	
	public static AlertRuleResponse from(AlertRule rule)
	{
		return new AlertRuleResponse(
				rule.getId(),
				rule.getName(),
				rule.getDescription(),
				rule.isEnabled(),
				rule.getEventType(),
				rule.getMinimumSeverity(),
				rule.getDeviceType(),
				rule.getAlertSeverity(),
				rule.getAlertTitle(),
				rule.getAlertMessage(),
				rule.getCreatedAt(),
				rule.getUpdatedAt()
				);
	}
	
	public UUID getId()
	{
		return id;
	}
	
	public String getName()
	{
		return name;
	}
	
	public String getDescription()
	{
		return description;
	}
	
	public boolean isEnabled()
	{
		return enabled;
	}
	
	public SecurityEventType getEventType()
	{
		return eventType;
	}
	
	public SecurityEventSeverity getMinimumSeverity()
	{
		return minimumSeverity;
	}
	
	public DeviceType getDeviceType()
	{
		return deviceType;
	}
	
	public AlertSeverity getAlertSeverity()
	{
		return alertSeverity;
	}
	
	public String getAlertTitle()
	{
		return alertTitle;
	}
	
	public String getAlertMessage()
	{
		return alertMessage;
	}
	
	public Instant getCreatedAt()
	{
		return createdAt;
	}
	
	public Instant getUpdatedAt()
	{
		return updatedAt;
	}
}
