package com.sentinelmesh.edge.util;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TimeUtils 
{
	private final Clock clock;
	
	public TimeUtils()
	{
		this.clock=Clock.systemUTC();
	}
	
	public TimeUtils(Clock clock)
	{
		if(clock==null)
			throw new IllegalArgumentException("Clock cannot be null");
		
		this.clock=clock;
	}
	
	public Instant now()
	{
		return Instant.now(clock);
	}
	
	/**
	 * Format Instant object to ISO8601 in UTC
	 * @param instant
	 * @return
	 */
	public String formatIso(Instant instant)
	{
		if(instant==null)
			throw new IllegalArgumentException("Instant cannot be null");
		
		return instant.toString();
	}
	
	public String formatIso(Instant instant, String zoneIdString)
	{
		if(instant==null)
			throw new IllegalArgumentException("Instant cannot be null");
		
		if(zoneIdString==null || zoneIdString.isBlank())
			throw new IllegalArgumentException("ZoneIdString cannot be null or blank");
		
		return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(instant.atZone(ZoneId.of(zoneIdString)));
	}
	
	public long secondsBetween(Instant start, Instant end)
	{
		if(start==null)
			throw new IllegalArgumentException("Start Instant cannot be null");
		
		if(end==null)
			throw new IllegalArgumentException("End Instant cannot be null");
		
		return Duration.between(start, end).toSeconds();
	}
}
