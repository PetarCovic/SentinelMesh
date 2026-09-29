package com.sentinelmesh.cameras;

import java.time.Instant;

public class CameraLiveState 
{
	private final CameraLiveStatus liveStatus;
	private final Instant latestLiveFrameTime;
	
	public CameraLiveState(CameraLiveStatus liveStatus, Instant latestLiveFrameTime) 
	{
		if(liveStatus==null)
			throw new IllegalArgumentException("CameraLiveStatus cannot be null");
		
		this.liveStatus = liveStatus;
		this.latestLiveFrameTime = latestLiveFrameTime;
	}

	public CameraLiveStatus getLiveStatus() 
	{
		return liveStatus;
	}

	public Instant getLatestLiveFrameTime()
	{
		return latestLiveFrameTime;
	}
}
