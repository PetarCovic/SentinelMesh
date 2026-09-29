package com.sentinelmesh.cameras;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sentinelmesh.devices.DeviceStatus;
import com.sentinelmesh.events.SecurityEventResponse;

public class CameraDetailsResponse 
{
	private final UUID deviceId;
	private final String name;
	private final String location;
	private final DeviceStatus deviceStatus;
	private final CameraLiveStatus liveStatus;
	private final Instant latestLiveFrameTime;
	private final boolean recordingAvailable;
	private final long recordingSegmentCount;
	private final Instant earliestRecordingTime;
	private final Instant latestRecordingTime;
	private final List<SecurityEventResponse> recentEvents;
	
	public CameraDetailsResponse(
			UUID deviceId,
			String name,
			String location,
			DeviceStatus deviceStatus,
			CameraLiveStatus liveStatus,
			Instant latestLiveFrameTime,
			boolean recordingAvailable,
			long recordingSegmentCount,
			Instant earliestRecordingTime,
			Instant latestRecordingTime,
			List<SecurityEventResponse> recentEvents
			)
	{
		if(deviceId==null)
			throw new IllegalArgumentException("DeviceId cannot be null");
		
		if(name==null)
			throw new IllegalArgumentException("Name cannot be null");
		
		if(deviceStatus==null)
			throw new IllegalArgumentException("DeviceStatus cannot be null");
		
		if(liveStatus==null)
			throw new IllegalArgumentException("LiveStatus cannot be null");
		
		if(recordingSegmentCount<0)
			throw new IllegalArgumentException("RecordingSegmentCount cannot be negative");
		
		if(recentEvents==null)
			throw new IllegalArgumentException("RecentEvents cannot be null");
	
		this.deviceId=deviceId;
		this.name=name;
		this.location=location;
		this.deviceStatus=deviceStatus;
		this.liveStatus=liveStatus;
		this.latestLiveFrameTime=latestLiveFrameTime;
		this.recordingAvailable=recordingAvailable;
		this.recordingSegmentCount=recordingSegmentCount;
		this.earliestRecordingTime=earliestRecordingTime;
		this.latestRecordingTime=latestRecordingTime;
		this.recentEvents=List.copyOf(recentEvents);
	}

	public UUID getDeviceId()
	{
		return deviceId;
	}

	public String getName()
	{
		return name;
	}

	public String getLocation()
	{
		return location;
	}

	public DeviceStatus getDeviceStatus()
	{
		return deviceStatus;
	}

	public CameraLiveStatus getLiveStatus() 
	{
		return liveStatus;
	}

	public Instant getLatestLiveFrameTime() 
	{
		return latestLiveFrameTime;
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
	
	public List<SecurityEventResponse> getRecentEvents()
	{
		return recentEvents;
	}
}