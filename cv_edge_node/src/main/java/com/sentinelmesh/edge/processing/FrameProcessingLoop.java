package com.sentinelmesh.edge.processing;

import java.util.List;

import com.sentinelmesh.edge.camera.Frame;
import com.sentinelmesh.edge.camera.FrameSource;
import com.sentinelmesh.edge.config.EdgeNodeConfig;
import com.sentinelmesh.edge.detection.DetectionResult;
import com.sentinelmesh.edge.detection.MotionDetector;

public class FrameProcessingLoop 
{
	private final FrameSource fs;
	private final MotionDetector motionDetector;
	private final EdgeNodeConfig config;
	private volatile boolean running=false;
	
	public FrameProcessingLoop(
			FrameSource fs,
			MotionDetector motionDetector,
			EdgeNodeConfig config
			)
	{
		if(fs==null)
			throw new IllegalArgumentException("Frame source cannot be null");
		
		if(motionDetector==null)
			throw new IllegalArgumentException("Motion detector cannot be null");
			
		if(config==null)
			throw new IllegalArgumentException("EdgeNodeConfig cannot be null");
		
		this.fs=fs;
		this.motionDetector=motionDetector;
		this.config=config;
	}
	
	public void start()
	{
		if(running)
			return;
		
		running=true;
		runLoop();
	}
	
	public void stop()
	{
		running=false;
	}
	
	private void runLoop()
	{
		Frame frame=new Frame();
		
		try
		{
			while(running)
			{
				boolean read=fs.read(frame);
				
				if(!read)
					break;
				
				processFrame(frame);
				
				sleepForTargetFps();
			}
		}
		finally
		{
			frame.close();
		}
	}
	
	private void processFrame(Frame frame)
	{
		if(!config.isMotionDetectionEnabled())
			return;
		
		List<DetectionResult> detections=motionDetector.detect(frame);
		
		if(detections.isEmpty())
			return;
		
		System.out.println("Number of detections: "+detections.size());

		System.out.println();
		for(DetectionResult detection : detections)
		{
			System.out.println(detection.toString());
		}
	}
	
	private void sleepForTargetFps()
	{
		int fps=config.getTargetFps();
		
		long msPerFrame=1000/fps;
		
		try
		{
			Thread.sleep(msPerFrame);
		}catch(InterruptedException ex)
		{
			Thread.currentThread().interrupt();
			running=false;
		}
	}
}
