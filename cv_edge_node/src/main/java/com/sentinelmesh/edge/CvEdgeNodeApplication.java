package com.sentinelmesh.edge;

import java.util.ArrayList;
import java.util.List;

import com.sentinelmesh.edge.camera.Frame;
import com.sentinelmesh.edge.camera.FrameSource;
import com.sentinelmesh.edge.camera.VideoFileFrameSource;
import com.sentinelmesh.edge.camera.WebcamFrameSource;
import com.sentinelmesh.edge.client.SecurityEventClient;
import com.sentinelmesh.edge.client.SentinelMeshApiClient;
import com.sentinelmesh.edge.config.ConfigLoader;
import com.sentinelmesh.edge.config.EdgeNodeConfig;
import com.sentinelmesh.edge.debug.FrameDebugViewer;
import com.sentinelmesh.edge.detection.DetectionPipeline;
import com.sentinelmesh.edge.detection.Detector;
import com.sentinelmesh.edge.detection.MotionDetector;
import com.sentinelmesh.edge.detection.PersonDetector;
import com.sentinelmesh.edge.processing.DetectionCooldownTracker;
import com.sentinelmesh.edge.processing.FrameProcessingLoop;
import com.sentinelmesh.edge.processing.FrameProcessor;
import com.sentinelmesh.edge.processing.HeartbeatLoop;
import com.sentinelmesh.edge.util.ShutdownHook;

public class CvEdgeNodeApplication
{
	private EdgeNodeConfig config;
	private FrameSource fs;
	private SentinelMeshApiClient apiClient;
	private HeartbeatLoop heartbeatLoop;
	private DetectionPipeline detectionPipeline;
	private MotionDetector motionDetector;
	private PersonDetector personDetector;
	private FrameProcessingLoop processingLoop;
	private DetectionCooldownTracker cooldownTracker;
	private SecurityEventClient securityEventClient;
	private FrameDebugViewer debugViewer;
	private ShutdownHook shutdownHook;
	private FrameProcessor frameProcessor;
	private volatile boolean shuttingDown=false;
	
	public static void main(String[] args)
	{
		CvEdgeNodeApplication app=new CvEdgeNodeApplication();
		
		app.run();
	}
	
	public void run()
	{
		config=new ConfigLoader().load();
		
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
			personDetector=new PersonDetector();
		
		boolean warmupSuccess=warmupMotionDetector(5);
		
		if(!warmupSuccess && config.isMotionDetectionEnabled())
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
		
		if(config.isDebugViewerEnabled())
			debugViewer=new FrameDebugViewer("SentinelMesh Motion Debug Viewer");
		
		List<Detector> detectors=new ArrayList<>();
		
		if(config.isMotionDetectionEnabled())
			detectors.add(motionDetector);
		if(config.isPersonDetectionEnabled())
			detectors.add(personDetector);
		
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
		if(config.isMotionDetectionEnabled())
			return false;
		
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
		
		
		frameProcessor=new FrameProcessor(
				detectionPipeline, 
				config, 
				cooldownTracker, 
				securityEventClient, 
				debugViewer);
		
		processingLoop=new FrameProcessingLoop(fs, frameProcessor, config);
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
		if(shuttingDown)
			return;
		
		shuttingDown=true;
		
		if(processingLoop!=null)
		{
			processingLoop.stop();
			processingLoop=null;
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
		
		if(apiClient!=null)
			apiClient=null;
		
		if(fs!=null)
		{
			fs.close();
			fs=null;
		}
		
		shutdownHook=null;
	}
}