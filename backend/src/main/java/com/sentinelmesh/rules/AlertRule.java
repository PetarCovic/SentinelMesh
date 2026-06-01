package com.sentinelmesh.rules;

import java.time.Instant;
import java.util.UUID;

import com.sentinelmesh.alerts.AlertSeverity;
import com.sentinelmesh.devices.DeviceType;
import com.sentinelmesh.events.SecurityEventSeverity;
import com.sentinelmesh.events.SecurityEventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name="alert_rules")
public class AlertRule 
{
	@Id
	@GeneratedValue(strategy=GenerationType.UUID)
	private UUID id;
	
	@Column(nullable=false, length=150)
	private String name;
	
	@Column(length=3000)
	private String description;
	
	@Column(nullable=false)
	private boolean enabled;
	
	@Enumerated(EnumType.STRING)
	@Column(name="event_type", length=50)
	private SecurityEventType eventType;
	
	@Enumerated(EnumType.STRING)
	@Column(name="minimum_severity", length=30)
	private SecurityEventSeverity minimumSeverity;
	
	@Enumerated(EnumType.STRING)
	@Column(name="device_type", length=50)
	private DeviceType deviceType;
	
	@Enumerated(EnumType.STRING)
	@Column(name="alert_severity", nullable=false, length=30)
	private AlertSeverity alertSeverity;
	
	@Column(name="alert_title", nullable=false, length=200)
	private String alertTitle;
	
	@Column(name="alert_message", nullable=false, length=1000)
	private String alertMessage;
	
	@Column(name="created_at", nullable=false)
	private Instant createdAt;
	
	@Column(name="updated_at", nullable=false)
	private Instant updatedAt;
	
	protected AlertRule()
	{
		
	}
	
	public AlertRule(
			String name,
			String description,
			boolean enabled,
			SecurityEventType eventType,
			SecurityEventSeverity minimumSeverity,
			DeviceType deviceType,
			AlertSeverity alertSeverity,
			String alertTitle,
			String alertMessage
			)
	{
		this.name=name;
		this.description=description;
		this.enabled=enabled;
		this.eventType=eventType;
		this.minimumSeverity=minimumSeverity;
		this.deviceType=deviceType;
		this.alertSeverity=alertSeverity;
		this.alertTitle=alertTitle;
		this.alertMessage=alertMessage;
	}
	
	@PrePersist
	public void prePersist()
	{
		Instant now=Instant.now();
		
		if(createdAt==null)
			createdAt=now;
		
		if(updatedAt==null)
			updatedAt=now;
	}
	
	@PreUpdate
	public void preUpdated()
	{
		updatedAt=Instant.now();
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
	
	public void update(
			String name,
			String description,
			Boolean enabled,
			SecurityEventType eventType,
			SecurityEventSeverity minimumSeverity,
			DeviceType deviceType,
			AlertSeverity alertSeverity,
			String alertTitle,
			String alertMessage
			)
	{
		if(name!=null)
			this.name=name;
		
		if(description!=null)
			this.description=description;
		
		if(enabled!=null)
			this.enabled=enabled;
		
		if(eventType!=null)
			this.eventType=eventType;
		
		if(minimumSeverity!=null)
			this.minimumSeverity=minimumSeverity;
		
		if(deviceType!=null)
			this.deviceType=deviceType;
		
		if(alertSeverity!=null)
			this.alertSeverity=alertSeverity;
		
		if(alertTitle!=null)
			this.alertTitle=alertTitle;
		
		if(alertMessage!=null)
			this.alertMessage=alertMessage;
	}
	
	public void disable()
	{
		this.enabled=false;
	}
	
	public void enable()
	{
		this.enabled=true;
	}
}
