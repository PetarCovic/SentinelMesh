package com.sentinelmesh.recordings;

import java.time.Instant;
import java.util.UUID;

public interface CameraRecordingAggregate
{
	public UUID getDeviceId();
	
	public Instant getEarliestRecordingTime();
	
	public Instant getLatestRecordingTime();
	
	public long getRecordingSegmentCount();
}
