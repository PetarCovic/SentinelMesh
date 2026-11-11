package com.sentinelmesh.recordings;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="sentinelmesh.recordings.reconciliation")
public class RecordingReconciliationProperties 
{
	private boolean enabled;
	private Duration interval;
	private Duration safetyWindow;
	
	public RecordingReconciliationProperties() 
	{
		this.enabled=false;
		this.interval=Duration.ofHours(6);
		this.safetyWindow=Duration.ofMinutes(10);
	}

	public boolean isEnabled() 
	{
		return enabled;
	}

	public void setEnabled(boolean enabled) 
	{
		this.enabled = enabled;
	}

	public Duration getInterval() 
	{
		return interval;
	}

	public void setInterval(Duration interval) 
	{
		if(interval==null)
			throw new IllegalArgumentException("Interval cannot be null");
		
		if(interval.isNegative() || interval.isZero())
			throw new IllegalArgumentException("Interval must be positive");
		
		this.interval = interval;
	}

	public Duration getSafetyWindow() 
	{
		return safetyWindow;
	}

	public void setSafetyWindow(Duration safetyWindow)
	{
		if(safetyWindow==null)
			throw new IllegalArgumentException("SafetyWindow cannot be null");
		
		if(safetyWindow.isNegative() || safetyWindow.isZero())
			throw new IllegalArgumentException("SafetyWindow must be positive");
		
		this.safetyWindow = safetyWindow;
	}
}
