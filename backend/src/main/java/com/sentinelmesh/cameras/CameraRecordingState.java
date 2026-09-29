package com.sentinelmesh.cameras;

import java.time.Instant;

public class CameraRecordingState 
{
	private final boolean recordingAvailable;
	private final long recordingSegmentCount;
	private final Instant earliestRecordingTime;
	private final Instant latestRecordingTime;
	
	public CameraRecordingState(
			long recordingSegmentCount, 
			Instant earliestRecordingTime,
			Instant latestRecordingTime
			) 
	{
		if(recordingSegmentCount<0)
			throw new IllegalArgumentException("RecordingSegmentCount cannot be negative");
		
		if(recordingSegmentCount==0)
		{
			this.recordingAvailable=false;
			this.recordingSegmentCount=0;
			
			if(earliestRecordingTime!=null)
				throw new IllegalArgumentException("EarliestRecordingTime must be null");
			
			if(latestRecordingTime!=null)
				throw new IllegalArgumentException("LatestRecordingTime must be null");
			
			this.earliestRecordingTime=null;
			this.latestRecordingTime=null;
		}
		else
		{		
			this.recordingAvailable=true;
			this.recordingSegmentCount=recordingSegmentCount;
			
			if(earliestRecordingTime==null)
				throw new IllegalArgumentException("EarliestRecordingTime cannot be null");
			
			if(latestRecordingTime==null)
				throw new IllegalArgumentException("LatestRecordingTime cannot be null");
			
			this.earliestRecordingTime=earliestRecordingTime;
			this.latestRecordingTime=latestRecordingTime;
		}
	}

	public boolean isRecordingAvailable() 
	{
		return recordingAvailable;
	}

	public long getRecordingSegmentCount() 
	{
		return recordingSegmentCount;
	}

	public Instant getEarliestRecordingTime()
	{
		return earliestRecordingTime;
	}

	public Instant getLatestRecordingTime()
	{
		return latestRecordingTime;
	}
}