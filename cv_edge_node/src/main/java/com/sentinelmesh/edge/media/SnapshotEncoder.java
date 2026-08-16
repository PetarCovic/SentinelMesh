package com.sentinelmesh.edge.media;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.global.opencv_imgcodecs;

import com.sentinelmesh.edge.camera.Frame;

public class SnapshotEncoder 
{
	public byte[] encodeJpeg(Frame frame)
	{
		if(frame==null || frame.isEmpty())
			throw new IllegalArgumentException("Frame cannot be null or empty");
		
		BytePointer outputBuffer=new BytePointer();
		try
		{
			boolean encoded=opencv_imgcodecs.imencode(".jpg", frame.getImage(), outputBuffer);
			
			if(!encoded)
				throw new IllegalStateException("Encoding failed");

			int limit=(int)outputBuffer.limit();
			
			if(limit<=0)
				throw new IllegalStateException("Limit must be greater than 0");
			
			byte[] jpegBytes=new byte[limit];
			
			for(int i=0; i<limit; i++)
			{
				jpegBytes[i]=outputBuffer.get(i);
			}
			
			return jpegBytes;
		}
		finally
		{
			outputBuffer.close();
		}
	}
}
