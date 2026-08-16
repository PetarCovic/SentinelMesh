package com.sentinelmesh.recordings;

import java.time.Instant;

public class CameraRecordingGapResponse
{
	private final Instant gapStart;
	private final Instant gapEnd;
	private final long durationSeconds;
	
	public CameraRecordingGapResponse(Instant gapStart, Instant gapEnd, long durationSeconds)
	{
		this.gapStart=gapStart;
		this.gapEnd=gapEnd;
		this.durationSeconds=durationSeconds;
	}
	
	public Instant getGapStart()
	{
		return gapStart;
	}
	
	public Instant getGapEnd()
	{
		return gapEnd;
	}
	
	public long getDurationSeconds()
	{
		return durationSeconds;
	}
}