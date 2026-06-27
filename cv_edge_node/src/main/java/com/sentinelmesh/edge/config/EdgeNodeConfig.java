package com.sentinelmesh.edge.config;

import java.util.UUID;

import com.sentinelmesh.edge.devices.DeviceType;
import com.sentinelmesh.edge.yolo.YoloModelMode;

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
	private final YoloModelMode yoloModelMode;
	private final String yoloWeightsPath;
	private final double personConfidenceThreshold;
	private final double yoloNmsThreshold;
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
			YoloModelMode yoloModelMode,
			String yoloWeightsPath,
			double personConfidenceThreshold,
			double yoloNmsThreshold,
			int detectionCooldownSeconds,
			int heartbeatIntervalSeconds,
			boolean enableMotionDetection,
			boolean enablePersonDetection,
			boolean enableDebugViewer
			)
	{
		if(backendBaseUrl==null || backendBaseUrl.isBlank())
			throw new IllegalArgumentException("Backend base url cannot be null or blank");
		
		if(deviceId==null)
			throw new IllegalArgumentException("Device Id cannot be null");
		
		if(apiKey==null || apiKey.isBlank())
			throw new IllegalArgumentException("Api key cannot be null or blank");
		
		if(deviceName==null || deviceName.isBlank())
			throw new IllegalArgumentException("Device name cannot be null or blank");
		
		if(deviceType==null)
			throw new IllegalArgumentException("Device type cannot be null");
		
		if(cameraIndex<0)
			throw new IllegalArgumentException("Camera index cannot be below 0");
		
		if(targetFps<=0)
			throw new IllegalArgumentException("Target fps must be greater than 0");
		
		if(motionThreshold<=0)
			throw new IllegalArgumentException("Motion threshold must be greater than 0");
		
		if(minimumContourArea<=0)
			throw new IllegalArgumentException("Minimum contour area must be greater than 0");
		
		if(yoloModelMode==null)
			throw new IllegalArgumentException("YoloModelMode cannot be null");
		
		if(personConfidenceThreshold<0 || personConfidenceThreshold>1)
			throw new IllegalArgumentException("PersonConfidenceThreshold must be between 0 and 1");
		
		if(yoloNmsThreshold<0 || yoloNmsThreshold>1)
			throw new IllegalArgumentException("YoloNmsThreshold must be between 0 and 1");
		
		if(detectionCooldownSeconds<0)
			throw new IllegalArgumentException("Detection cooldown seconds cannot be less than 0");
		
		if(heartbeatIntervalSeconds<=0)
			throw new IllegalArgumentException("Heartbeat interval seconds must be greater than 0");
		
		
		this.backendBaseUrl=backendBaseUrl;	
		this.deviceId=deviceId;
		this.apiKey=apiKey;
		this.deviceName=deviceName;
		this.deviceType=deviceType;
		this.deviceLocation=deviceLocation;
		this.cameraIndex=cameraIndex;
		this.videoFilePath=videoFilePath;
		this.targetFps=targetFps;
		this.motionThreshold=motionThreshold;
		this.minimumContourArea=minimumContourArea;
		this.yoloModelMode=yoloModelMode;
		this.yoloWeightsPath=yoloWeightsPath;
		this.personConfidenceThreshold=personConfidenceThreshold;
		this.yoloNmsThreshold=yoloNmsThreshold;
		this.detectionCooldownSeconds=detectionCooldownSeconds;
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
	
	public YoloModelMode getYoloModelMode()
	{
		return yoloModelMode;
	}
	
	public String getYoloWeightsPath()
	{
		return yoloWeightsPath;
	}
	
	public double getPersonConfidenceThreshold()
	{
		return personConfidenceThreshold;
	}
	
	public double getYoloNmsThreshold()
	{
		return yoloNmsThreshold;
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
