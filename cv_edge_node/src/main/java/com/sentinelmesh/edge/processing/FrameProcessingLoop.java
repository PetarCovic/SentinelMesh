package com.sentinelmesh.edge.processing;

import com.sentinelmesh.edge.camera.Frame;
import com.sentinelmesh.edge.camera.FrameSource;
import com.sentinelmesh.edge.config.EdgeNodeConfig;
import com.sentinelmesh.edge.debug.FrameDebugViewer;

public class FrameProcessingLoop 
{
	private final FrameSource fs;
	private final FrameProcessor frameProcessor;
	private final EdgeNodeConfig config;
	
	private volatile boolean running=false;
	
	public FrameProcessingLoop(
			FrameSource fs,
			FrameProcessor frameProcessor,
			EdgeNodeConfig config
			)
	{
		if(fs==null)
			throw new IllegalArgumentException("Frame source cannot be null");
		
		if(frameProcessor==null)
			throw new IllegalArgumentException("Frame Processor cannot be null");
			
		if(config==null)
			throw new IllegalArgumentException("EdgeNodeConfig cannot be null");
		
		this.fs=fs;
		this.frameProcessor=frameProcessor;
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
				
				try
				{
					frameProcessor.process(frame);
				}catch(Exception ex)
				{
					System.out.println("Frame Processing failed: "+ex.getMessage());
				}
				
				sleepForTargetFps();
			}
		}
		finally
		{
			frame.close();
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
