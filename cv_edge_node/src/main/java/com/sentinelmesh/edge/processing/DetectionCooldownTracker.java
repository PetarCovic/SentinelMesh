package com.sentinelmesh.edge.processing;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.sentinelmesh.edge.detection.DetectionResult;
import com.sentinelmesh.edge.detection.DetectionType;

public class DetectionCooldownTracker 
{
	private final Map<DetectionType, Instant> lastSeenAt;
	private int cooldownSeconds;
	
	public DetectionCooldownTracker()
	{
		this(30);
	}
	
	public DetectionCooldownTracker(int cooldownSeconds)
	{
		if(cooldownSeconds<0)
			throw new IllegalArgumentException("CooldownSeconds cannot be negative");
		
		this.lastSeenAt=new HashMap<DetectionType, Instant>();
		this.cooldownSeconds=cooldownSeconds;
	}
	
	public boolean shouldAllow(DetectionResult result)
	{
		if(result==null)
			throw new IllegalArgumentException("Result cannot be null");
		
		Instant lastSeenInstant=lastSeenAt.get(result.getType());
		Duration cooldownDuration=Duration.ofSeconds(cooldownSeconds);
		
		if(!lastSeenAt.containsKey(result.getType()))
			return true;
		else if(!lastSeenInstant.plus(cooldownDuration).isAfter(Instant.now()))
			return true;	
		else
			return false;
	}
	
	public boolean shouldAllowAndMarkSent(DetectionResult result)
	{
		if(shouldAllow(result))
		{
			markSent(result);
			return true;
		}
		
		return false;
	}
	
	public void markSent(DetectionResult result)
	{
		if(result==null)
			throw new IllegalArgumentException("Result cannot be null");
		
		lastSeenAt.put(result.getType(), Instant.now());
	}
	
	public void reset()
	{
		lastSeenAt.clear();
	}
}
