package com.sentinelmesh.rules;

import com.sentinelmesh.alerts.AlertSeverity;
import com.sentinelmesh.devices.DeviceType;
import com.sentinelmesh.events.SecurityEventSeverity;
import com.sentinelmesh.events.SecurityEventType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateAlertRuleRequest 
{
	@NotBlank(message="Rule name is required")
	@Size(max=150, message="Rule must be at most 150 characters")
	private String name;
	
	@Size(max=1000, message="Description must be at most 1000 characters")
	private String description;
	
	private boolean enabled=true;
	
	private SecurityEventType eventType;
	
	private SecurityEventSeverity minimumSeverity;
	
	private DeviceType deviceType;
	
	@NotNull(message="Alert severity is required")
	private AlertSeverity alertSeverity;
	
	@NotBlank(message="Alert title is required")
	@Size(max=200, message="Alert title must be at most 200 characters")
	private String alertTitle;
	
	@NotBlank(message="Alert message is required")
	@Size(max=1000, message="Alert message must be at most 1000 characters")
	private String alertMessage;
	
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
	
	public void setName(String name) {
	    this.name = name;
	}

	public void setDescription(String description) 
	{
	    this.description = description;
	}
	
	public void enable()
	{
		this.enabled=true;
	}
	
	public void disable()
	{
		this.enabled=false;
	}

	public void setEventType(SecurityEventType eventType) 
	{
	    this.eventType = eventType;
	}

	public void setMinimumSeverity(SecurityEventSeverity minimumSeverity) 
	{
	    this.minimumSeverity = minimumSeverity;
	}

	public void setDeviceType(DeviceType deviceType) 
	{
	    this.deviceType = deviceType;
	}

	public void setAlertSeverity(AlertSeverity alertSeverity) 
	{
	    this.alertSeverity = alertSeverity;
	}

	public void setAlertTitle(String alertTitle) 
	{
	    this.alertTitle = alertTitle;
	}

	public void setAlertMessage(String alertMessage) 
	{
	    this.alertMessage = alertMessage;
	}
}
