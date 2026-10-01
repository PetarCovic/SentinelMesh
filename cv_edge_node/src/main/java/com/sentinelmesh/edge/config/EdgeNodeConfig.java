package com.sentinelmesh.edge.config;

import java.nio.file.Path;
import java.time.Duration;
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
	private final boolean enableDetectionBoxes;
	private final boolean enableContinuousRecording;
	private final boolean enableRetainUploadedRecordings;
	private final boolean enableLiveDetectionOverlay;
	private final int eventClipFPS;
	private final int eventClipBufferSeconds;
	private final long recordingSegmentDurationSeconds;
	private final int recordingSegmentFPS;
	private final Path recordingSpoolRoot;
	private final int recordingMaximumUploadAttempts;
	private final long recordingMaximumSpoolSizeBytes;
	private final long recordingMinimumFreeDiskSpaceBytes;
	private final int recordingWidth;
	private final int recordingHeight;
	private final boolean deletePendingWhenNecessary;
	private final Duration recordingUploadInterval;
	private final Duration uploadedRecordingRetentionDuration;
	private final Duration uploadedRecordingCleanupInterval;
	private final int livePreviewFPS;

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
			boolean enableDebugViewer,
			boolean enableDetectionBoxes,
			boolean enableContinuousRecording,
			boolean enableRetainUploadedRecordings,
			boolean enableLiveDetectionOverlay,
			int eventClipFPS,
			int eventClipBufferSeconds,
			long recordingSegmentDurationSeconds,
			int recordingSegmentFPS,
			Path recordingSpoolRoot,
			int recordingMaximumUploadAttempts,
			long recordingMaximumSpoolSizeBytes,
			long recordingMinimumFreeDiskSpaceBytes,
			int recordingWidth,
			int recordingHeight,
			boolean deletePendingWhenNecessary,
			Duration recordingUploadInterval,
			Duration uploadedRecordingRetentionDuration,
			Duration uploadedRecordingCleanupInterval,
			int livePreviewFPS
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

		if(eventClipFPS<=0)
			throw new IllegalArgumentException("EventClipFPS must be greater than 0");

		if(eventClipBufferSeconds<=0)
			throw new IllegalArgumentException("EventClipBufferSeconds must be greater than 0");

		if(recordingSegmentDurationSeconds<=0)
			throw new IllegalArgumentException("RecordingSegmentDurationSeconds must be greater than 0");

		if(recordingSegmentFPS<=0)
			throw new IllegalArgumentException("RecordingSegmentFPS must be greater than 0");

		if(recordingSpoolRoot==null || recordingSpoolRoot.toString().isBlank())
			throw new IllegalArgumentException("RecordingSpoolRoot cannot be null or empty");

		if(recordingMaximumUploadAttempts<=0)
			throw new IllegalArgumentException("RecordingMaximumUploadAttempts must be greater "
					+ "than 0");

		if(recordingMaximumSpoolSizeBytes<=0)
			throw new IllegalArgumentException("RecordingMaximumSpoolSizeBytes must be greater "
					+ "than 0");

		if(recordingMinimumFreeDiskSpaceBytes<0)
			throw new IllegalArgumentException("RecordingMinimumFreeDiskSpaceBytes cannot be "
					+ "negative");

		if(recordingWidth<=0)
			throw new IllegalArgumentException("RecordingWidth must be greater than 0");

		if(recordingHeight<=0)
			throw new IllegalArgumentException("RecordingHeight must be greater than 0");

		if(recordingUploadInterval==null)
			throw new IllegalArgumentException("RecordingUploadInterval cannot be null");

		if(uploadedRecordingRetentionDuration==null)
			throw new IllegalArgumentException("UploadedRecordingRetentionDuration cannot be null");

		if(uploadedRecordingCleanupInterval==null)
			throw new IllegalArgumentException("UploadedRecordingCleanupInterval cannot be null");

		if(!recordingUploadInterval.isPositive())
			throw new IllegalArgumentException("RecordingUploadInterval must be positive");

		if(recordingUploadInterval.compareTo(Duration.ofSeconds(1)) < 0)
			throw new IllegalArgumentException("RecordingUploadInterval must be at least 1 second");

		if(uploadedRecordingRetentionDuration.isNegative())
			throw new IllegalArgumentException("UploadedRecordingRetentionDuration cannot be negative");

		if(uploadedRecordingCleanupInterval.compareTo(Duration.ofSeconds(1)) < 0)
			throw new IllegalArgumentException("UploadedRecordingCleanupInterval must be at least 1 second");

		if(livePreviewFPS<=0)
			throw new IllegalArgumentException("LivePreviewFPS must be greater than 0");

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
		this.enableContinuousRecording=enableContinuousRecording;
		this.enableDetectionBoxes=enableDetectionBoxes;
		this.enableRetainUploadedRecordings=enableRetainUploadedRecordings;
		this.enableLiveDetectionOverlay=enableLiveDetectionOverlay;
		this.eventClipFPS=eventClipFPS;
		this.eventClipBufferSeconds=eventClipBufferSeconds;
		this.recordingSegmentDurationSeconds=recordingSegmentDurationSeconds;
		this.recordingSegmentFPS=recordingSegmentFPS;
		this.recordingSpoolRoot=recordingSpoolRoot;
		this.recordingMaximumUploadAttempts=recordingMaximumUploadAttempts;
		this.recordingMaximumSpoolSizeBytes=recordingMaximumSpoolSizeBytes;
		this.recordingMinimumFreeDiskSpaceBytes=recordingMinimumFreeDiskSpaceBytes;
		this.recordingWidth=recordingWidth;
		this.recordingHeight=recordingHeight;
		this.deletePendingWhenNecessary=deletePendingWhenNecessary;
		this.recordingUploadInterval=recordingUploadInterval;
		this.uploadedRecordingRetentionDuration=uploadedRecordingRetentionDuration;
		this.uploadedRecordingCleanupInterval=uploadedRecordingCleanupInterval;
		this.livePreviewFPS=livePreviewFPS;
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

	public boolean isDetectionBoxesEnabled()
	{
		return enableDetectionBoxes;
	}

	public boolean isContinuousRecordingEnabled()
	{
		return enableContinuousRecording;
	}

	public boolean isRetainUploadedRecordingsEnabled()
	{
		return enableRetainUploadedRecordings;
	}

	public boolean isLiveDetectionOverlayEnabled()
	{
		return enableLiveDetectionOverlay;
	}

	public boolean hasVideoFilePath()
	{
		return videoFilePath!=null && !videoFilePath.isBlank();
	}

	public int getEventClipFPS()
	{
		return eventClipFPS;
	}

	public int getEventClipBufferSeconds()
	{
		return eventClipBufferSeconds;
	}

	public long getRecordingSegmentDurationSeconds()
	{
		return recordingSegmentDurationSeconds;
	}

	public int getRecordingSegmentFPS()
	{
		return recordingSegmentFPS;
	}

	public Path getRecordingSpoolRoot()
	{
		return recordingSpoolRoot;
	}

	public int getRecordingMaximumUploadAttempts()
	{
		return recordingMaximumUploadAttempts;
	}

	public long getRecordingMaximumSpoolSizeBytes()
	{
		return recordingMaximumSpoolSizeBytes;
	}

	public long getRecordingMinimumFreeDiskSpaceBytes()
	{
		return recordingMinimumFreeDiskSpaceBytes;
	}

	public int getRecordingWidth()
	{
		return recordingWidth;
	}

	public int getRecordingHeight()
	{
		return recordingHeight;
	}

	public boolean isDeletePendingWhenNecessary()
	{
		return deletePendingWhenNecessary;
	}

	public Duration getRecordingUploadInterval()
	{
		return recordingUploadInterval;
	}

	public Duration getUploadedRecordingRetentionDuration()
	{
		return uploadedRecordingRetentionDuration;
	}

	public Duration getUploadedRecordingCleanupInterval()
	{
		return uploadedRecordingCleanupInterval;
	}

	public int getLivePreviewFPS()
	{
		return livePreviewFPS;
	}
}