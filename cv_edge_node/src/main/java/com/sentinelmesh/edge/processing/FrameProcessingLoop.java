package com.sentinelmesh.edge.processing;

import com.sentinelmesh.edge.camera.Frame;
import com.sentinelmesh.edge.camera.FrameSource;
import com.sentinelmesh.edge.config.EdgeNodeConfig;
import com.sentinelmesh.edge.live.LiveFramePublisher;
import com.sentinelmesh.edge.recording.ContinuousRecordingSubsystem;

public class FrameProcessingLoop 
{
	private final FrameSource fs;
	private final FrameProcessor frameProcessor;
	private final EdgeNodeConfig config;
	private final ContinuousRecordingSubsystem recordingSubsystem;
	private final LiveFramePublisher liveFramePublisher;
	private volatile Thread processingThread;
	
	private volatile boolean running=false;
	
	public FrameProcessingLoop(
			FrameSource fs,
			FrameProcessor frameProcessor,
			EdgeNodeConfig config,
			ContinuousRecordingSubsystem recordingSubsystem,
			LiveFramePublisher liveFramePublisher
			)
	{
		if(fs==null)
			throw new IllegalArgumentException("Frame source cannot be null");
		
		if(frameProcessor==null)
			throw new IllegalArgumentException("Frame Processor cannot be null");
			
		if(config==null)
			throw new IllegalArgumentException("EdgeNodeConfig cannot be null");
		
		if(recordingSubsystem==null)
			throw new IllegalArgumentException("RecordingSubsystem cannot be null");
		
		if(liveFramePublisher==null)
			throw new IllegalArgumentException("LiveFramePublisher cannot be null");
		
		this.fs=fs;
		this.frameProcessor=frameProcessor;
		this.config=config;
		this.recordingSubsystem=recordingSubsystem;
		this.liveFramePublisher=liveFramePublisher;
	}
	
	public void start()
	{
	    if(running)
	        return;

	    running = true;
	    processingThread = Thread.currentThread();

	    try
	    {
	        runLoop();
	    }
	    finally
	    {
	        processingThread = null;
	    }
	}
	
	public void stop()
	{
	    running = false;

	    Thread thread = processingThread;

	    if(thread != null && thread != Thread.currentThread())
	        thread.interrupt();
	}
	
	public void awaitTermination()
	{
	    Thread thread = processingThread;

	    if(thread == null || thread == Thread.currentThread())
	        return;

	    try
	    {
	        thread.join();
	    }
	    catch(InterruptedException ex)
	    {
	        Thread.currentThread().interrupt();
	    }
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
				{
					running=false;
					break;
				}
				
				try
				{
					recordingSubsystem.acceptFrame(frame);
				}
				catch(Exception ex)
				{
					System.err.println("Recording Subsystem failed: "+ex.getMessage());
				}
				
				try
				{
					liveFramePublisher.acceptFrame(frame);
				}
				catch(Exception ex)
				{
					System.err.println("LiveFramePublisher failed: "+ex.getMessage());
				}
				
				try
				{
					frameProcessor.process(frame);
				}catch(Exception ex)
				{
					System.err.println("Frame Processing failed: "+ex.getMessage());
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
	    int fps = config.getTargetFps();
	    long msPerFrame = 1000L / fps;

	    try
	    {
	        Thread.sleep(msPerFrame);
	    }
	    catch(InterruptedException ex)
	    {
	    	// stop() interrupts the processing thread so it exits sleep immediately.
	        running = false;
	    }
	}
}
