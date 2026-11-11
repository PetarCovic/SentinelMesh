package com.sentinelmesh.edge.camera;

import java.time.Instant;

import org.bytedeco.opencv.opencv_core.Mat;

public class Frame 
{
	private int frameId;
	private Instant timestamp;
	private Mat image;
	private String sourceName;
	private long captureNanos;
	
	public Frame()
	{
		image=new Mat();
	}
	
	public Frame(
			Mat image,
			int frameId,
			Instant timestamp,
			String sourceName,
			long captureNanos
			)
	{
		this.frameId=frameId;
		this.timestamp=timestamp;
		this.image=image;
		this.sourceName=sourceName;
		this.captureNanos=captureNanos;
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
	
	public long getCaptureNanos()
	{
		return captureNanos;
	}
	
	public boolean isEmpty()
	{
		return image==null || image.empty();
	}
	
	public Frame copy()
	{
		if(isEmpty())
			throw new IllegalStateException("Cannot copy an empty frame");

		Mat imageCopy = image.clone();

		return new Frame(
				imageCopy,
				frameId,
				timestamp,
				sourceName,
				captureNanos
		);
	}
	
	public void update(
			int frameId, 
			Instant timestamp,
			String sourceName,
			long captureNanos)
	{
		this.frameId=frameId;
		this.captureNanos=captureNanos;
		
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