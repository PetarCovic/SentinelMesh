package com.sentinelmesh.edge.config;

import java.util.UUID;

import com.sentinelmesh.edge.devices.DeviceType;
import com.sentinelmesh.edge.yolo.YoloModelMode;

public class ConfigLoader 
{
	//TODO Maybe make configuration file
	private final String SENTINEL_BACKEND_BASE_URL="http://localhost:8080";
	private final UUID SENTINEL_DEVICE_ID=UUID.fromString("9f5c15be-45b1-4687-957b-4ea5493465b7");
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
	private YoloModelMode SENTINEL_YOLO_MODEL_MODE=YoloModelMode.TRAINED_WEIGHTS;
	private String SENTINEL_YOLO_WEIGHTS_PATH="models/yolo-v1/yolo-v1-person.weights";
	private double SENTINEL_PERSON_CONFIDENCE_THRESHOLD=.07;
	private double SENTINEL_YOLO_NMS_THRESHOLD=.45;
	private int SENTINEL_DETECTION_COOLDOWN_SECONDS=10;
	private int SENTINEL_HEARTBEAT_INTERVAL_SECONDS=10;
	private boolean SENTINEL_ENABLE_MOTION_DETECTION=false;
	private boolean SENTINEL_ENABLE_PERSON_DETECTION=true;
	private boolean SENTINEL_ENABLE_DEBUG_VIEWER = true;
	
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
				SENTINEL_YOLO_MODEL_MODE,
				SENTINEL_YOLO_WEIGHTS_PATH,
				SENTINEL_PERSON_CONFIDENCE_THRESHOLD,
				SENTINEL_YOLO_NMS_THRESHOLD,
				SENTINEL_DETECTION_COOLDOWN_SECONDS,
				SENTINEL_HEARTBEAT_INTERVAL_SECONDS,
				SENTINEL_ENABLE_MOTION_DETECTION,
				SENTINEL_ENABLE_PERSON_DETECTION,
				SENTINEL_ENABLE_DEBUG_VIEWER
				);
	}
}