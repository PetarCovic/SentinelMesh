package com.sentinelmesh.edge.camera;

import java.time.Instant;

import org.bytedeco.opencv.opencv_core.Mat;

public class Frame 
{
	private int frameId;
	private Instant timestamp;
	private Mat image;
	private String sourceName;
	
	public Frame()
	{
		image=new Mat();
	}
	
	public int getFrameId()
	{
		return frameId;
	}
	
	public Instant getTimestamp()
	{
		return timestamp;
	}
	
	public Mat getImage()
	{
		return image;
	}
	
	public int getWidth()
	{
		return image.cols();
	}
	
	public int getHeight()
	{
		return image.rows();
	}
	
	public String getSourceName()
	{
		return sourceName;
	}
	
	public boolean isEmpty()
	{
		return image==null || image.empty();
	}
	
	public void update(
			int frameId, 
			Instant timestamp,
			String sourceName)
	{
		this.frameId=frameId;
		
		if(timestamp!=null)
			this.timestamp=timestamp;
		
		if(sourceName!=null)
			this.sourceName=sourceName;
	}
	
	public void close()
	{
		image.release();
		image=null;
	}
}