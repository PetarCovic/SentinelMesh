package com.sentinelmesh.edge.config;

import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.sentinelmesh.edge.devices.DeviceType;
import com.sentinelmesh.edge.yolo.YoloModelMode;

public class ConfigLoader 
{
	//TODO Maybe make configuration file
	private static final String SENTINEL_BACKEND_BASE_URL="http://[::1]:8080";
	private static final UUID SENTINEL_DEVICE_ID=UUID.fromString("9f5c15be-45b1-4687-957b-4ea5493465b7");
	private static final String SENTINEL_DEVICE_API_KEY =
	        java.util.Objects.requireNonNull(
	                System.getenv("SENTINEL_DEVICE_API_KEY"),
	                "SENTINEL_DEVICE_API_KEY must be set");
	private static final String SENTINEL_DEVICE_NAME="Local Webcam";
	private static final DeviceType SENTINEL_DEVICE_TYPE=DeviceType.CAMERA;
	private static final String SENTINEL_DEVICE_LOCATION="Unknown";
	private static final int SENTINEL_CAMERA_INDEX=0;
	private static final String SENTINEL_VIDEO_FILE_PATH=null;
	private static final int SENTINEL_TARGET_FPS=10;
	private static final double SENTINEL_MOTION_THRESHOLD=25.0;
	private static final double SENTINEL_MINIMUM_CONTOUR_AREA=750.0;
	private static final YoloModelMode SENTINEL_YOLO_MODEL_MODE=YoloModelMode.TRAINED_WEIGHTS;
	private static final String SENTINEL_YOLO_WEIGHTS_PATH="models/yolo-v1/yolo-v1-person.weights";
	private static final double SENTINEL_PERSON_CONFIDENCE_THRESHOLD=.07;
	private static final double SENTINEL_YOLO_NMS_THRESHOLD=.45;
	private static final int SENTINEL_DETECTION_COOLDOWN_SECONDS=10;
	private static final int SENTINEL_HEARTBEAT_INTERVAL_SECONDS=10;
	private static final boolean SENTINEL_ENABLE_MOTION_DETECTION=true;
	private static final boolean SENTINEL_ENABLE_PERSON_DETECTION=false;
	private static final boolean SENTINEL_ENABLE_DEBUG_VIEWER = true;
	private static final boolean SENTINEL_ENABLE_DETECTION_BOXES=true;
	private static final boolean SENTINEL_ENABLE_CONTINUOUS_RECORDING=true;
	private static final boolean SENTINEL_ENABLE_RETAIN_UPLOADED_RECORDINGS=true;
	private static final int SENTINEL_EVENT_CLIP_FPS=10;
	private static final int SENTINEL_EVENT_CLIP_BUFFER_SECONDS=10;
	private static final int SENTINEL_RECORDING_SEGMENT_DURATION_SECONDS=60;
	private static final int SENTINEL_RECORDING_SEGMENT_FPS=10;
	private static final Path SENTINEL_RECORDING_SPOOL_ROOT=Path.of("./recording-spool");
	private static final int SENTINEL_RECORDING_MAXIMUM_UPLOAD_ATTEMPTS=10;
	private static final long SENTINEL_RECORDING_MAXIMUM_SPOOL_SIZE_BYTES=10737418240L; //10*1024*1024*1024
	private static final long SENTINEL_RECORDING_MINIMUM_FREE_DISK_SPACE_BYTES=2147483648L; //20*1024*1024*1024
	private static final int SENTINEL_RECORDING_WIDTH=640;
	private static final int SENTINEL_RECORDING_HEIGHT=480;
	private static final boolean SENTINEL_DELETE_PENDING_WHEN_NECESSARY=false;
	private static final Duration SENTINEL_RECORDING_UPLOAD_INTERVAL=Duration.ofSeconds(10);
	private static final Duration SENTINEL_UPLOADED_RECORDING_RETENTION_DURATION=Duration.ofMinutes(1);//Duration.ofDays(1);
	private static final Duration SENTINEL_UPLOADED_RECORDING_CLEANUP_INTERVAL=Duration.ofSeconds(10);//Duration.ofHours(1);
	private static final int SENTINEL_LIVE_PREVIEW_FPS=10;
	
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
				SENTINEL_ENABLE_DEBUG_VIEWER,
				SENTINEL_ENABLE_DETECTION_BOXES,
				SENTINEL_ENABLE_CONTINUOUS_RECORDING,
				SENTINEL_ENABLE_RETAIN_UPLOADED_RECORDINGS,
				SENTINEL_EVENT_CLIP_FPS,
				SENTINEL_EVENT_CLIP_BUFFER_SECONDS,
				SENTINEL_RECORDING_SEGMENT_DURATION_SECONDS,
				SENTINEL_RECORDING_SEGMENT_FPS,
				SENTINEL_RECORDING_SPOOL_ROOT,
				SENTINEL_RECORDING_MAXIMUM_UPLOAD_ATTEMPTS,
				SENTINEL_RECORDING_MAXIMUM_SPOOL_SIZE_BYTES,
				SENTINEL_RECORDING_MINIMUM_FREE_DISK_SPACE_BYTES,
				SENTINEL_RECORDING_WIDTH,
				SENTINEL_RECORDING_HEIGHT,
				SENTINEL_DELETE_PENDING_WHEN_NECESSARY,
				SENTINEL_RECORDING_UPLOAD_INTERVAL,
				SENTINEL_UPLOADED_RECORDING_RETENTION_DURATION,
				SENTINEL_UPLOADED_RECORDING_CLEANUP_INTERVAL,
				SENTINEL_LIVE_PREVIEW_FPS
				);
	}
}