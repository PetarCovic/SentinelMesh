package com.sentinelmesh.edge.config;

import java.util.UUID;

import com.sentinelmesh.edge.devices.DeviceType;

public class ConfigLoader 
{
	//TODO Maybe make configuration file
	private final String SENTINEL_BACKEND_BASE_URL="http://localhost:8080";
	private final UUID SENTINEL_DEVICE_ID=UUID.fromString("573c8225-38de-4c38-85a4-c457a09b6b33");
	private final String SENTINEL_DEVICE_API_KEY=
			"sm_redacted_rotated";
	private String SENTINEL_DEVICE_NAME="Local Webcam";
	private DeviceType SENTINEL_DEVICE_TYPE=DeviceType.CAMERA;
	private String SENTINEL_DEVICE_LOCATION="Unknown";
	private int SENTINEL_CAMERA_INDEX=0;
	private String SENTINEL_VIDEO_FILE_PATH=null;
	private int SENTINEL_TARGET_FPS=10;
	private double SENTINEL_MOTION_THRESHOLD=25.0;
	private double SENTINEL_MINIMUM_CONTOUR_AREA=750.0;
	private int SENTINEL_DETECTION_COOLDOWN_SECONDS=10;
	private int SENTINEL_HEARTBEAT_INTERVAL_SECONDS=10;
	private boolean SENTINEL_ENABLE_MOTION_DETECTION=true;
	private boolean SENTINEL_ENABLE_PERSON_DETECTION=false;
	
	public EdgeNodeConfig load()
	{
		return loadDefault();
	}
	
	public EdgeNodeConfig loadDefault()
	{
		return new EdgeNodeConfig(
				SENTINEL_BACKEND_BASE_URL.trim(), 
				SENTINEL_DEVICE_ID, 
				SENTINEL_DEVICE_API_KEY,
				SENTINEL_DEVICE_NAME,
				SENTINEL_DEVICE_TYPE,
				SENTINEL_DEVICE_LOCATION,
				SENTINEL_CAMERA_INDEX,
				SENTINEL_VIDEO_FILE_PATH,
				SENTINEL_TARGET_FPS,
				SENTINEL_MOTION_THRESHOLD,
				SENTINEL_MINIMUM_CONTOUR_AREA,
				SENTINEL_DETECTION_COOLDOWN_SECONDS,
				SENTINEL_HEARTBEAT_INTERVAL_SECONDS,
				SENTINEL_ENABLE_MOTION_DETECTION,
				SENTINEL_ENABLE_PERSON_DETECTION
				);
	}
}