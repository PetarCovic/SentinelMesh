package com.sentinelmesh.recordings;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="sentinelmesh.recordings.retention")
public class RecordingRetentionProperties 
{
	private boolean enabled;
	private Duration duration;
	private Duration cleanupInterval;
	private int batchSize;
	
	public RecordingRetentionProperties()
	{
		this.enabled=false;
		this.duration=Duration.ofDays(30);
		this.cleanupInterval=Duration.ofHours(1);
		this.batchSize=100;
	}
	
	public RecordingRetentionProperties(
			boolean enabled, 
			Duration duration,
			Duration cleanupInterval, 
			int batchSize
			) 
	{
		if(duration==null)
			throw new IllegalArgumentException("Duration cannot be null");
		
		if(duration.isNegative() || duration.isZero())
			throw new IllegalArgumentException("Duration must be positive");
		
		if(cleanupInterval==null)
			throw new IllegalArgumentException("CleanupInterval cannot be null");
		
		if(cleanupInterval.isNegative() || cleanupInterval.isZero())
			throw new IllegalArgumentException("CleanupInterval must be positive");
		
		if(batchSize<1)
			throw new IllegalArgumentException("BatchSize must be greater than or equal to 1");
		
		this.enabled = enabled;
		this.duration = duration;
		this.cleanupInterval = cleanupInterval;
		this.batchSize = batchSize;
	}

	public boolean isEnabled()
	{
		return enabled;
	}

	public Duration getDuration() 
	{
		return duration;
	}

	public Duration getCleanupInterval() 
	{
		return cleanupInterval;
	}

	public int getBatchSize() 
	{
		return batchSize;
	}

	public void setEnabled(boolean enabled) 
	{		
		this.enabled = enabled;
	}

	public void setDuration(Duration duration) 
	{
		if(duration==null)
			throw new IllegalArgumentException("Duration cannot be null");
		
		if(duration.isNegative() || duration.isZero())
			throw new IllegalArgumentException("Duration must be positive");
		
		this.duration = duration;
	}

	public void setCleanupInterval(Duration cleanupInterval)
	{
		if(cleanupInterval==null)
			throw new IllegalArgumentException("CleanupInterval cannot be null");
		
		if(cleanupInterval.isNegative() || cleanupInterval.isZero())
			throw new IllegalArgumentException("CleanupInterval must be positive");
		
		this.cleanupInterval = cleanupInterval;
	}

	public void setBatchSize(int batchSize)
	{
		if(batchSize<1)
			throw new IllegalArgumentException("BatchSize must be greater than or equal to 1");
		
		this.batchSize = batchSize;
	}
}
