package com.sentinelmesh.async;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="sentinelmesh.event-processing")
public class EventProcessingProperties 
{
	private String queueName="sentinelmesh:event-processing";
	private boolean enabled=true;
	
	public String getQueueName()
	{
		return queueName;
	}
	
	public boolean isEnabled()
	{
		return enabled;
	}
	
	public void setQueueName(String queueName)
	{
		this.queueName=queueName;
	}
	
	public void setEnabled(boolean enabled)
	{
        this.enabled = enabled;
    }
	
	public void enable()
	{
		this.enabled=true;
	}
	
	public void disable()
	{
		this.enabled=false;
	}
}
