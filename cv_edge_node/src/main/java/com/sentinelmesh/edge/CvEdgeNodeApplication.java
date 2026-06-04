package com.sentinelmesh.edge;

import com.sentinelmesh.edge.camera.Frame;
import com.sentinelmesh.edge.camera.FrameSource;
import com.sentinelmesh.edge.camera.VideoFileFrameSource;
import com.sentinelmesh.edge.camera.WebcamFrameSource;
import com.sentinelmesh.edge.config.ConfigLoader;
import com.sentinelmesh.edge.config.EdgeNodeConfig;

public class CvEdgeNodeApplication
{
	public static void main(String[] args)
	{
		CvEdgeNodeApplication app=new CvEdgeNodeApplication();
		
		app.run();
	}
	
	public void run()
	{
		EdgeNodeConfig config=new ConfigLoader().load();
		FrameSource fs=createFrameSource(config);
		runFrameSourceSmokeTest(fs);
		//startHeartbeatLoop();
		//startProcessingLoop();
		//shutdown();
	}
	
	public void runFrameSourceSmokeTest(FrameSource fs)
	{
		if(fs==null)
			throw new IllegalArgumentException("Frame source cannot be null");
		
		System.out.println(fs.getSourceName());
		
		boolean open=fs.open();
		
		if(!open)
		{
			System.out.println("Could not open frame source: "+fs.getSourceName());
			return;
		}
		
		Frame frame=new Frame();
		
		try
		{
			boolean success=fs.read(frame);
			
			if(!success || frame.isEmpty())
			{
				System.out.println("Error capturing frame");
				return;
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
			fs.close();
		}
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
	
	public void startHeartbeatLoop()
	{
		
	}
	
	public void startProcessingLoop()
	{
		
	}
	
	public void shutdown()
	{
		
	}
}
