package com.sentinelmesh.edge.config;

import java.util.UUID;

import com.sentinelmesh.edge.devices.DeviceType;

public class EdgeNodeConfig 
{
	private final String backendBaseUrl;
	private final UUID deviceId;
	private final String apiKey;
	private final String deviceName;
	private final DeviceType deviceType;
	private final String deviceLocation;
	private final int cameraIndex;
	private final String videoFilePath;
	private final int targetFps;
	private final double motionThreshold;
	private final double minimumContourArea;
	private final int detectionCooldownSeconds;
	private final int heartbeatIntervalSeconds;
	private final boolean enableMotionDetection;
	private final boolean enablePersonDetection;
	private final boolean enableDebugViewer;
	
	public EdgeNodeConfig(
			String backendBaseUrl,
			UUID deviceId,
			String apiKey,
			String deviceName,
			DeviceType deviceType,
			String deviceLocation,
			int cameraIndex,
			String videoFilePath,
			int targetFps,
			double motionThreshold,
			double minimumContourArea,
			int detectionCooldownSeconds,
			int heartbeatIntervalSeconds,
			boolean enableMotionDetection,
			boolean enablePersonDetection,
			boolean enableDebugViewer
			)
	{
		if(backendBaseUrl==null || backendBaseUrl.isBlank())
			throw new IllegalArgumentException("Backend base url cannot be null or blank");
		this.backendBaseUrl=backendBaseUrl;	
		
		if(deviceId==null)
			throw new IllegalArgumentException("Device Id cannot be null");
		this.deviceId=deviceId;

		if(apiKey==null || apiKey.isBlank())
			throw new IllegalArgumentException("Api key cannot be null or blank");
		this.apiKey=apiKey;

		if(deviceName==null || deviceName.isBlank())
			throw new IllegalArgumentException("Device name cannot be null or blank");
		this.deviceName=deviceName;

		if(deviceType==null)
			throw new IllegalArgumentException("Device type cannot be null");
		this.deviceType=deviceType;

		this.deviceLocation=deviceLocation;

		if(cameraIndex<0)
			throw new IllegalArgumentException("Camera index cannot be below 0");
		this.cameraIndex=cameraIndex;
		
		this.videoFilePath=videoFilePath;

		if(targetFps<=0)
			throw new IllegalArgumentException("Target fps must be greater than 0");
		this.targetFps=targetFps;

		if(motionThreshold<=0)
			throw new IllegalArgumentException("Motion threshold must be greater than 0");
		this.motionThreshold=motionThreshold;

		if(minimumContourArea<=0)
			throw new IllegalArgumentException("Minimum contour area must be greater than 0");
		this.minimumContourArea=minimumContourArea;

		if(detectionCooldownSeconds<0)
			throw new IllegalArgumentException("Detection cooldown seconds cannot be less than 0");
		this.detectionCooldownSeconds=detectionCooldownSeconds;

		if(heartbeatIntervalSeconds<=0)
			throw new IllegalArgumentException("Heartbeat interval seconds must be greater than 0");
		
		this.heartbeatIntervalSeconds=heartbeatIntervalSeconds;
		
		this.enableMotionDetection=enableMotionDetection;
		
		this.enablePersonDetection=enablePersonDetection;
		
		this.enableDebugViewer=enableDebugViewer;
	}
	
	public String getBackendBaseUrl()
	{
		return backendBaseUrl;
	}
	
	public UUID getDeviceId()
	{
		return deviceId;
	}
	
	public String getApiKey()
	{
		return apiKey;
	}
	
	public String getDeviceName()
	{
		return deviceName;
	}
	
	public DeviceType getDeviceType()
	{
		return deviceType;
	}
	
	public String getDeviceLocation()
	{
		return deviceLocation;
	}
	
	public int getCameraIndex()
	{
		return cameraIndex;
	}
	
	public String getVideoFilePath()
	{
		return videoFilePath;
	}
	
	public int getTargetFps()
	{
		return targetFps;
	}
	
	public double getMotionThreshold()
	{
		return motionThreshold;
	}
	
	public double getMinimumContourArea()
	{
		return minimumContourArea;
	}
	
	public int getDetectionCooldownSeconds()
	{
		return detectionCooldownSeconds;
	}
	
	public int getHeartbeatIntervalSeconds()
	{
		return heartbeatIntervalSeconds;
	}
	
	public boolean isMotionDetectionEnabled()
	{
		return enableMotionDetection;
	}
	
	public boolean isPersonDetectionEnabled()
	{
		return enablePersonDetection;
	}
	
	public boolean isDetectionEnabled()
	{
		return enableMotionDetection
				|| enablePersonDetection;
	}
	
	public boolean isDebugViewerEnabled()
	{
		return enableDebugViewer;
	}
	
	public boolean hasVideoFilePath()
	{
		return videoFilePath!=null && !videoFilePath.isBlank();
	}
}
