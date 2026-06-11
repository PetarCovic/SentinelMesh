package com.sentinelmesh.edge.yolo;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;

import com.sentinelmesh.edge.camera.Frame;

public class YoloPreprocessor 
{
	public YoloInput preprocess(Frame frame)
	{
		if(frame==null || frame.isEmpty())
			throw new IllegalArgumentException("Frame cannot be null or empty");
		
		Mat source=frame.getImage();
		int originalWidth=frame.getWidth();
		int originalHeight=frame.getHeight();
		int inputWidth=448;
		int inputHeight=448;
		
		Mat resized=new Mat();
		float[][][] imageData=new float[inputHeight][inputWidth][3];
		
		try
		{
			opencv_imgproc.resize(source, resized, new Size(inputWidth, inputHeight));
			
			if(resized.channels()!=3)
				throw new IllegalArgumentException("Channel count must be equal to 3");
			
			for(int r=0; r<resized.rows(); r++)
			{
				for(int c=0; c<resized.cols(); c++)
				{
					BytePointer data=resized.ptr(r, c);
					
					imageData[r][c][0]=(data.get(2) & 0xFF)/255.0f; //R
					imageData[r][c][1]=(data.get(1) & 0xFF)/255.0f; //G
					imageData[r][c][2]=(data.get(0) & 0xFF)/255.0f; //B
				}
			}
		}
		finally
		{
			resized.release();
		}
		
		return new YoloInput(imageData, originalWidth, originalHeight, inputWidth, inputHeight);
	}
}
