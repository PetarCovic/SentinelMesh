package com.sentinelmesh.recordings;

import java.time.Instant;

public interface CameraRecordingTimelineAggregate 
{
	public Instant getEarliestRecordingTime();
	
	public Instant getLatestRecordingTime();
	
	public long getTotalSegmentCount();
	
	public long getTotalRecordedDurationSeconds();
}