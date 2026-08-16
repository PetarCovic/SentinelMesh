package com.sentinelmesh.recordings;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sentinelmesh.common.PageResponse;

public class CameraRecordingTimelineResponse 
{
	private final UUID deviceId;
	private final Instant requestedStart;
	private final Instant requestedEnd;
	private final Instant earliestRecordingTime;
	private final Instant latestRecordingTime;
	private final long totalSegmentCount;
	private final long totalRecordedDurationSeconds;
	private final PageResponse<RecordingSegmentResponse> segments;
	private final List<CameraRecordingGapResponse> gaps;
	
	public CameraRecordingTimelineResponse(
			UUID deviceId,
			Instant requestedStart,
			Instant requestedEnd,
			Instant earliestRecordingTime,
			Instant latestRecordingTime,
			long totalSegmentCount,
			long totalRecordedDurationSeconds,
			PageResponse<RecordingSegmentResponse> segments,
			List<CameraRecordingGapResponse> gaps
			)
	{
		if(segments==null)
			throw new IllegalArgumentException("Segments cannot be null");
		
		if(gaps==null)
			throw new IllegalArgumentException("Gaps cannot be null");
			
		this.deviceId=deviceId;
		this.requestedStart=requestedStart;
		this.requestedEnd=requestedEnd;
		this.earliestRecordingTime=earliestRecordingTime;
		this.latestRecordingTime=latestRecordingTime;
		this.totalSegmentCount=totalSegmentCount;
		this.totalRecordedDurationSeconds=totalRecordedDurationSeconds;
		this.segments=segments;
		this.gaps=List.copyOf(gaps);
	}
	
	public UUID getDeviceId()
	{
		return deviceId;
	}
	
	public Instant getRequestedStart()
	{
		return requestedStart;
	}
	
	public Instant getRequestedEnd()
	{
		return requestedEnd;
	}
	
	public Instant getEarliestRecordingTime()
	{
		return earliestRecordingTime;
	}
	
	public Instant getLatestRecordingTime()
	{
		return latestRecordingTime;
	}
	
	public long getTotalSegmentCount()
	{
		return totalSegmentCount;
	}
	
	public long getTotalRecordedDurationSeconds()
	{
		return totalRecordedDurationSeconds;
	}
	
	public PageResponse<RecordingSegmentResponse> getSegments()
	{
		return segments;
	}
	
	public List<CameraRecordingGapResponse> getGaps()
	{
		return gaps;
	}
}