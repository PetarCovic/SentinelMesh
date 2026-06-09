package com.sentinelmesh.edge;

import com.sentinelmesh.edge.camera.Frame;
import com.sentinelmesh.edge.camera.FrameSource;
import com.sentinelmesh.edge.camera.VideoFileFrameSource;
import com.sentinelmesh.edge.camera.WebcamFrameSource;
import com.sentinelmesh.edge.client.SecurityEventClient;
import com.sentinelmesh.edge.client.SentinelMeshApiClient;
import com.sentinelmesh.edge.config.ConfigLoader;
import com.sentinelmesh.edge.config.EdgeNodeConfig;
import com.sentinelmesh.edge.detection.MotionDetector;
import com.sentinelmesh.edge.processing.DetectionCooldownTracker;
import com.sentinelmesh.edge.processing.FrameProcessingLoop;
import com.sentinelmesh.edge.processing.HeartbeatLoop;

public class CvEdgeNodeApplication
{
	private EdgeNodeConfig config;
	private FrameSource fs;
	private SentinelMeshApiClient apiClient;
	private HeartbeatLoop heartbeatLoop;
	private MotionDetector motionDetector;
	private FrameProcessingLoop processingLoop;
	private DetectionCooldownTracker cooldownTracker;
	private SecurityEventClient securityEventClient;
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
		
		motionDetector=new MotionDetector(
				config.getMotionThreshold(), 
				config.getMinimumContourArea());
		
		boolean warmupSuccess=warmupMotionDetector(5);
		
		if(!warmupSuccess)
		{
			shutdown();
			return;
		}
		
		Runtime.getRuntime().addShutdownHook(new Thread(()-> shutdown(), "cv-edge-node-shutdown"));
		
		apiClient=new SentinelMeshApiClient(config.getBackendBaseUrl());
		startHeartbeatLoop(apiClient, config);
		
		cooldownTracker=new DetectionCooldownTracker(config.getDetectionCooldownSeconds());
		securityEventClient=new SecurityEventClient(apiClient);
		
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
		
		if(motionDetector==null)
			throw new IllegalArgumentException("Motion detector cannot be null");
		
		if(config==null)
			throw new IllegalArgumentException("EdgeNodeConfig cannot be null");
		
		if(cooldownTracker==null)
			throw new IllegalArgumentException("CooldownTracker cannot be null");
		
		if(securityEventClient==null)
			throw new IllegalArgumentException("SecurityEventClient cannot be null");
		
		processingLoop=new FrameProcessingLoop(
				fs, 
				motionDetector, 
				config, 
				cooldownTracker,
				securityEventClient);
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
		
		if(cooldownTracker!=null)
		{
			cooldownTracker.reset();
			cooldownTracker=null;
		}
		
		if(securityEventClient!=null)
		{
			securityEventClient=null;
		}
		
		if(heartbeatLoop!=null)
		{	
			heartbeatLoop.stop();
			heartbeatLoop=null;
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
	}
}