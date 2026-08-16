package com.sentinelmesh.edge;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.sentinelmesh.edge.camera.Frame;
import com.sentinelmesh.edge.camera.FrameSource;
import com.sentinelmesh.edge.camera.VideoFileFrameSource;
import com.sentinelmesh.edge.camera.WebcamFrameSource;
import com.sentinelmesh.edge.client.LiveFrameUploadClient;
import com.sentinelmesh.edge.client.RecordingSegmentUploadClient;
import com.sentinelmesh.edge.client.SecurityEventClient;
import com.sentinelmesh.edge.client.SentinelMeshApiClient;
import com.sentinelmesh.edge.client.SnapshotUploadClient;
import com.sentinelmesh.edge.client.VideoClipUploadClient;
import com.sentinelmesh.edge.config.ConfigLoader;
import com.sentinelmesh.edge.config.EdgeNodeConfig;
import com.sentinelmesh.edge.debug.FrameDebugViewer;
import com.sentinelmesh.edge.detection.DetectionPipeline;
import com.sentinelmesh.edge.detection.Detector;
import com.sentinelmesh.edge.detection.MotionDetector;
import com.sentinelmesh.edge.detection.PersonDetectionWorker;
import com.sentinelmesh.edge.detection.PersonDetector;
import com.sentinelmesh.edge.live.LiveFramePublisher;
import com.sentinelmesh.edge.media.FrameRingBuffer;
import com.sentinelmesh.edge.media.SnapshotEncoder;
import com.sentinelmesh.edge.media.VideoClipEncoder;
import com.sentinelmesh.edge.processing.DetectionCooldownTracker;
import com.sentinelmesh.edge.processing.FrameProcessingLoop;
import com.sentinelmesh.edge.processing.FrameProcessor;
import com.sentinelmesh.edge.processing.HeartbeatLoop;
import com.sentinelmesh.edge.recording.ContinuousRecordingSubsystem;
import com.sentinelmesh.edge.util.ShutdownHook;
import com.sentinelmesh.edge.yolo.YoloDetectionResultMapper;
import com.sentinelmesh.edge.yolo.YoloPostProcessor;
import com.sentinelmesh.edge.yolo.YoloPreprocessor;
import com.sentinelmesh.edge.yolo.v1.YoloV1Model;
import com.sentinelmesh.edge.yolo.v1.YoloV1ModelFactory;
import com.sentinelmesh.edge.yolo.v1.YoloV1OutputDecoder;

public class CvEdgeNodeApplication
{
	private EdgeNodeConfig config;
	private FrameSource fs;
	private SentinelMeshApiClient apiClient;
	private HeartbeatLoop heartbeatLoop;
	private DetectionPipeline detectionPipeline;
	private MotionDetector motionDetector;
	private PersonDetector personDetector;
	private PersonDetectionWorker personDetectionWorker;
	private FrameProcessingLoop processingLoop;
	private DetectionCooldownTracker cooldownTracker;
	private SecurityEventClient securityEventClient;
	private FrameDebugViewer debugViewer;
	private ShutdownHook shutdownHook;
	private FrameProcessor frameProcessor;
	private SnapshotEncoder snapshotEncoder;
	private SnapshotUploadClient snapshotUploadClient;
	private FrameRingBuffer frameRingBuffer;
	private VideoClipEncoder videoClipEncoder;
	private VideoClipUploadClient videoClipUploadClient;
	private ContinuousRecordingSubsystem recordingSubsystem;
	private LiveFrameUploadClient liveFrameUploadClient;
	private LiveFramePublisher liveFramePublisher;
	
	private volatile boolean shuttingDown=false;
	
	public static void main(String[] args)
	{
		CvEdgeNodeApplication app=new CvEdgeNodeApplication();
		
		app.run();
	}
	
	public void run()
	{
		config=new ConfigLoader().load();
		
		if(config==null)
			throw new IllegalStateException("EdgeNodeConfig cannot be null");
		
		fs=createFrameSource(config);
		boolean startupSuccess=startupFrameSource();
		
		if(!startupSuccess)
		{
			shutdown();
			return;
		}
		
		if(config.isMotionDetectionEnabled())
			motionDetector=new MotionDetector(
					config.getMotionThreshold(), 
					config.getMinimumContourArea());
		
		if(config.isPersonDetectionEnabled())
		{
			personDetector=createPersonDetector();
			personDetectionWorker=new PersonDetectionWorker(personDetector);
		}
		
		boolean warmupSuccess=warmupMotionDetector(5);
		
		if(!warmupSuccess)
		{
			shutdown();
			return;
		}
		
		shutdownHook=new ShutdownHook("cv-edge-node-shutdown", ()->shutdown());
		shutdownHook.register();
		
		apiClient=new SentinelMeshApiClient(config.getBackendBaseUrl());
		startHeartbeatLoop(apiClient, config);
		
		cooldownTracker=new DetectionCooldownTracker(config.getDetectionCooldownSeconds());
		securityEventClient=new SecurityEventClient(apiClient);
		
		snapshotEncoder=new SnapshotEncoder();
		snapshotUploadClient=new SnapshotUploadClient(apiClient);
		
		int eventClipFps=config.getEventClipFPS();
		int eventClipBufferSeconds=config.getEventClipBufferSeconds();
		int maxClipFrames = eventClipFps * eventClipBufferSeconds;

		frameRingBuffer = new FrameRingBuffer(maxClipFrames);
		videoClipEncoder = new VideoClipEncoder(eventClipFps);
		videoClipUploadClient = new VideoClipUploadClient(apiClient);
		
		if(config.isDebugViewerEnabled())
			debugViewer=new FrameDebugViewer("SentinelMesh Motion Debug Viewer");
		
		List<Detector> detectors=new ArrayList<>();
		
		if(config.isMotionDetectionEnabled())
			detectors.add(motionDetector);
		
		if(detectors.isEmpty() && !config.isPersonDetectionEnabled())
			System.out.println("No detection features enabled. "
					+ "Edge node will run without detections");
		else if(detectors.isEmpty())
			System.out.println("No synchronous detectors enabled. "
					+ "Edge node will use async person detection only.");
		
		detectionPipeline=new DetectionPipeline(detectors);
		
		
				
		try
		{
			startProcessingLoop();
		}
		finally
		{
			shutdown();
		}
	}
	
	private boolean startupFrameSource()
	{
		if(fs==null)
			throw new IllegalArgumentException("Frame Source cannot be null");
		
		System.out.println("Opening frame source: "+fs.getSourceName());

		boolean open=fs.open();
			
		if(!open)
		{
			System.out.println("Could not open frame source: "+fs.getSourceName());
			return false;
		}
		
		Frame frame=new Frame();
		
		try
		{
			boolean success=fs.read(frame);
			
			if(!success || frame.isEmpty())
			{
				System.out.println("Error capturing frame");
				return false;
			}
			
			System.out.println("Captured frame successfully.");
			System.out.println("Frame ID: "+frame.getFrameId());
			System.out.println("Timestamp: "+frame.getTimestamp());
			System.out.println("Source name: "+frame.getSourceName());
	        System.out.println("Frame width: " + frame.getWidth());
	        System.out.println("Frame height: " + frame.getHeight());
		}finally
		{
	        frame.close();
		}
        
        return true;
	}
	
	private FrameSource createFrameSource(EdgeNodeConfig config)
	{		
		if(config==null)
			throw new IllegalArgumentException("Config can not be null");
		
		if(config.hasVideoFilePath())
			return new VideoFileFrameSource(config.getVideoFilePath());
		else
			return new WebcamFrameSource(config.getCameraIndex());
	}
	
	private void startHeartbeatLoop(SentinelMeshApiClient apiClient, EdgeNodeConfig config)
	{
		if(apiClient==null)
			throw new IllegalArgumentException("SentinelMeshApiClient cannot be null");
		
		if(config==null)
			throw new IllegalArgumentException("EdgeNodeConfig cannot be null");
		
		this.heartbeatLoop=new HeartbeatLoop(apiClient, config);
		
		heartbeatLoop.start();
	}
	
	private boolean warmupMotionDetector(int frameCount)
	{
		if(!config.isMotionDetectionEnabled())
			return true;
		
		if(fs == null)
			throw new IllegalArgumentException("Frame source cannot be null");
		
		if(motionDetector == null)
			throw new IllegalArgumentException("Motion detector cannot be null");
		
		if(frameCount <= 0)
			throw new IllegalArgumentException("Frame count must be greater than 0");
		
		Frame frame = new Frame();
		
		try
		{
			int warmedUpFrames = 0;
			
			while(warmedUpFrames < frameCount)
			{
				boolean success = fs.read(frame);
				
				if(!success || frame.isEmpty())
				{
					System.out.println("Failed to read calibration frame");
					return false;
				}
				
				motionDetector.warmup(frame);
				warmedUpFrames++;
				
				sleepForWarmupFrame();
				
				if(Thread.currentThread().isInterrupted())
				{
					System.out.println("Motion detector warmup interrupted");
					return false;
				}
			}
			
			System.out.println("Motion detector warmed up with " + warmedUpFrames + " frames.");
			return true;
		}
		finally
		{
			frame.close();
		}
	}
	
	private void startProcessingLoop()
	{
		if(fs==null)
			throw new IllegalArgumentException("Frame source cannot be null");
		
		if(detectionPipeline==null)
			throw new IllegalArgumentException("DetectionPipeline cannot be null");
		
		if(config==null)
			throw new IllegalArgumentException("EdgeNodeConfig cannot be null");
		
		if(cooldownTracker==null)
			throw new IllegalArgumentException("CooldownTracker cannot be null");
		
		if(securityEventClient==null)
			throw new IllegalArgumentException("SecurityEventClient cannot be null");
		
		if(snapshotEncoder==null)
		    throw new IllegalArgumentException("SnapshotEncoder cannot be null");

		if(snapshotUploadClient==null)
		    throw new IllegalArgumentException("SnapshotUploadClient cannot be null");

		if(frameRingBuffer==null)
		    throw new IllegalArgumentException("FrameRingBuffer cannot be null");

		if(videoClipEncoder==null)
		    throw new IllegalArgumentException("VideoClipEncoder cannot be null");

		if(videoClipUploadClient==null)
		    throw new IllegalArgumentException("VideoClipUploadClient cannot be null");
		
		frameProcessor=new FrameProcessor(
				detectionPipeline, 
				personDetectionWorker,
				config, 
				cooldownTracker, 
				securityEventClient, 
				debugViewer,
				snapshotEncoder,
				snapshotUploadClient,
				frameRingBuffer,
				videoClipEncoder,
				videoClipUploadClient
				);
		
		RecordingSegmentUploadClient recordingUploadClient=
				new RecordingSegmentUploadClient(apiClient);
		
		recordingSubsystem=new ContinuousRecordingSubsystem(config, recordingUploadClient);
		
		liveFrameUploadClient=new LiveFrameUploadClient(apiClient);
		
		liveFramePublisher=new LiveFramePublisher(
				liveFrameUploadClient,
				snapshotEncoder,
				config.getDeviceId(),
				config.getApiKey(),
				config.getLivePreviewFPS());
		
		liveFramePublisher.start();
		
		processingLoop=new FrameProcessingLoop(
				fs, 
				frameProcessor, 
				config, 
				recordingSubsystem,
				liveFramePublisher
				);
		recordingSubsystem.start();
		processingLoop.start();
	}
	
	private void sleepForWarmupFrame()
	{
		int fps = config.getTargetFps();
		long msPerFrame = 1000 / fps;
		
		try
		{
			Thread.sleep(msPerFrame);
		}
		catch(InterruptedException ex)
		{
			Thread.currentThread().interrupt();
		}
	}
	
	public void shutdown()
	{	
		synchronized(this)
		{
			if(shuttingDown)
				return;
			
			shuttingDown=true;
		}
		
		if(processingLoop!=null)
		{
			processingLoop.stop();
			processingLoop.awaitTermination();
			processingLoop=null;
		}
		
		if(recordingSubsystem!=null)
		{
			recordingSubsystem.close();
			recordingSubsystem=null;
		}
		
		if(liveFramePublisher!=null)
		{
			liveFramePublisher.close();
			liveFramePublisher=null;
		}
		
		if(frameProcessor != null)
		    frameProcessor = null;

		if(detectionPipeline != null)
		    detectionPipeline = null;
		
		if(debugViewer!=null)
		{
			debugViewer.close();
			debugViewer=null;
		}
		
		if(cooldownTracker!=null)
		{
			cooldownTracker.reset();
			cooldownTracker=null;
		}
		
		if(heartbeatLoop!=null)
		{	
			heartbeatLoop.stop();
			heartbeatLoop=null;
		}
		
		if(securityEventClient!=null)
		{
			securityEventClient=null;
		}
		
		if(motionDetector!=null)
		{
			motionDetector.close();
			motionDetector=null;
		}
		
		if(personDetectionWorker != null)
		{
			personDetectionWorker.close();
			personDetectionWorker = null;
			personDetector = null;
		}
		else if(personDetector != null)
		{
			personDetector.close();
			personDetector = null;
		}
		
		if(apiClient!=null)
			apiClient=null;
		
		if(snapshotEncoder!=null)
			snapshotEncoder=null;
		
		if(snapshotUploadClient!=null)
			snapshotUploadClient=null;
		
		if(frameRingBuffer!=null)
		{
			frameRingBuffer.close();
			frameRingBuffer=null;
		}
		
		if(videoClipEncoder!=null)
			videoClipEncoder=null;
		
		if(videoClipUploadClient!=null)
			videoClipUploadClient=null;
		
		if(fs!=null)
		{
			fs.close();
			fs=null;
		}
		
		shutdownHook=null;
	}
	
	private PersonDetector createPersonDetector()
	{
		YoloV1Model model;

		switch(config.getYoloModelMode())
		{
		    case TINY_TEST:
		        model = YoloV1ModelFactory.createTinyTestModel();
		        break;

		    case RANDOM_TEST:
		        model = YoloV1ModelFactory.createRandomPersonModel();
		        break;

		    case TRAINED_WEIGHTS:
		        model = YoloV1ModelFactory.createFromWeights(
		                Path.of(config.getYoloWeightsPath())
		        );
		        break;

		    default:
		        throw new IllegalStateException("Unsupported YOLO model mode: " 
		    + config.getYoloModelMode());
		}
		
		System.out.println("Creating person detector with YOLO mode: " + config.getYoloModelMode());
		
		return new PersonDetector(
		        new YoloPreprocessor(),
		        model,
		        new YoloV1OutputDecoder("person"),
		        new YoloPostProcessor(
		                config.getPersonConfidenceThreshold(),
		                config.getYoloNmsThreshold()
		        ),
		        new YoloDetectionResultMapper()
		);
	}
}