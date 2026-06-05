package com.sentinelmesh.edge;

import com.sentinelmesh.edge.camera.Frame;
import com.sentinelmesh.edge.camera.FrameSource;
import com.sentinelmesh.edge.camera.VideoFileFrameSource;
import com.sentinelmesh.edge.camera.WebcamFrameSource;
import com.sentinelmesh.edge.client.SentinelMeshApiClient;
import com.sentinelmesh.edge.config.ConfigLoader;
import com.sentinelmesh.edge.config.EdgeNodeConfig;
import com.sentinelmesh.edge.detection.MotionDetector;
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
		
		apiClient=new SentinelMeshApiClient(config.getBackendBaseUrl());
		startHeartbeatLoop(apiClient, config);
		
		motionDetector=new MotionDetector(
				config.getMotionThreshold(), 
				config.getMinimumContourArea());
		
		boolean warmupSuccess=warmupMotionDetector(5);
		
		if(!warmupSuccess)
		{
			shutdown();
			return;
		}
		
		
		try
		{
			startProcessingLoop();
		}
		finally
		{
			shutdown();
		}
	}
	
	public boolean startupFrameSource()
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
	
	public FrameSource createFrameSource(EdgeNodeConfig config)
	{		
		if(config==null)
			throw new IllegalArgumentException("Config can not be null");
		
		if(config.hasVideoFilePath())
			return new VideoFileFrameSource(config.getVideoFilePath());
		else
			return new WebcamFrameSource(config.getCameraIndex());
	}
	
	public void startHeartbeatLoop(SentinelMeshApiClient apiClient, EdgeNodeConfig config)
	{
		if(apiClient==null)
			throw new IllegalArgumentException("SentinelMeshApiClient cannot be null");
		
		if(config==null)
			throw new IllegalArgumentException("EdgeNodeConfig cannot be null");
		
		this.heartbeatLoop=new HeartbeatLoop(apiClient, config);
		
		heartbeatLoop.start();
	}
	
	public boolean warmupMotionDetector(int frameCount)
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
	
	public void startProcessingLoop()
	{
		if(fs==null)
			throw new IllegalArgumentException("Frame source cannot be null");
		
		if(motionDetector==null)
			throw new IllegalArgumentException("Motion detector cannot be null");
		
		if(config==null)
			throw new IllegalArgumentException("EdgeNodeConfig cannot be null");
		
		processingLoop=new FrameProcessingLoop(fs, motionDetector, config);
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
		if(processingLoop!=null)
		{
			processingLoop.stop();
			processingLoop=null;
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
		
		//Shutdown other resources
		
		if(fs!=null)
		{
			fs.close();
			fs=null;
		}
	}
}