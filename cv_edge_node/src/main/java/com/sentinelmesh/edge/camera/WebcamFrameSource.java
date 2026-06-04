package com.sentinelmesh.edge.camera;

import java.time.Instant;

import org.bytedeco.opencv.opencv_videoio.VideoCapture;

import com.sentinelmesh.edge.exceptions.FrameSourceNotOpenException;

public class WebcamFrameSource implements FrameSource
{
	private final int cameraIndex;
	private int frameCounter=1;
	private VideoCapture capture;
	
	public WebcamFrameSource(int cameraIndex)
	{
		this.cameraIndex=cameraIndex;
	}

	@Override
	public boolean open() 
	{
		capture=new VideoCapture(cameraIndex);
		
		boolean opened=capture.isOpened();
		
		if(!opened)
		{
			capture.release();
			capture=null;
		}
		
		return opened;
	}

	@Override
	public boolean read(Frame frame)
	{
		if(!isOpen())
		{
			throw new FrameSourceNotOpenException(getSourceName());
		}
		
		if(frame==null)
		{
			throw new IllegalArgumentException("Frame cannot be null");
		}
		
		boolean read=capture.read(frame.getImage());
		
		if(!read)
		{
			System.out.println("Error reading from capture device");
			return false;
		}
		
		if(frame.isEmpty())
		{
			System.out.println("Error assigning frame");
			return false;
		}
		
		frame.update(frameCounter, Instant.now(), getSourceName());
		frameCounter++;
		
		return true;
	}

	@Override
	public boolean isOpen() 
	{
		return capture!=null && capture.isOpened();
	}

	@Override
	public void close() 
	{
		if(capture!=null)
		{
			capture.release();
			capture=null;
		}
	}

	@Override
	public String getSourceName() 
	{
		return "webcam:"+cameraIndex;
	}
}
