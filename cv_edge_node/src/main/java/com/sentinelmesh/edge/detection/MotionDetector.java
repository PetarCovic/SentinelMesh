package com.sentinelmesh.edge.detection;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.global.opencv_video;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_video.BackgroundSubtractorMOG2;

import com.sentinelmesh.edge.camera.Frame;

public class MotionDetector 
{
	private final double motionThreshold;
	private final double minimumContourArea;
	private final BackgroundSubtractorMOG2 backSub;
	private final Mat morphKernel;
	private final NonMaxSuppression nonMaxSuppression;
	
	public MotionDetector(
			double motionThreshold, 
			double minimumContourArea)
	{
		if(motionThreshold<=0)
			throw new IllegalArgumentException("Motion threshold must be greater than 0");
		
		if(minimumContourArea<=0)
			throw new IllegalArgumentException("Minimum contour area must be greater than 0");
		
		this.motionThreshold=motionThreshold;
		this.minimumContourArea=minimumContourArea;
		
		this.backSub=opencv_video.createBackgroundSubtractorMOG2(16, motionThreshold, false);
		
		this.morphKernel=
				opencv_imgproc.getStructuringElement(opencv_imgproc.MORPH_RECT, new Size(9, 9));
		
		this.nonMaxSuppression=new NonMaxSuppression();
	}
	
	public List<DetectionResult> detect(Frame frame)
	{	
		validateFrame(frame);
		
		Mat fgMask=new Mat();
		Mat motionMask=null;
		
		try
		{
			backSub.apply(frame.getImage(), fgMask);
			
			motionMask=getMotionMask(fgMask);
			
			List<MotionRegion> motionRegions=findMotionRegions(motionMask);

			if(motionRegions.isEmpty())
				return List.of();
			
			return buildDetectionResults(frame, motionRegions);
		}finally
		{
			fgMask.release();
			
			if(motionMask!=null)
				motionMask.release();
		}
	}
	
	public void warmup(Frame frame)
	{
		validateFrame(frame);

		Mat fgMask = new Mat();

		try
		{
			backSub.apply(frame.getImage(), fgMask);
		}
		finally
		{
			fgMask.release();
		}
	}
	
	private Mat getMotionMask(Mat fgMask)
	{
		if(fgMask==null || fgMask.empty())
			throw new IllegalArgumentException("Foreground mask cannot be null or empty");
		
		Mat thresholdMat=new Mat();
		Mat motionMask=new Mat();
		
		try
		{
			opencv_imgproc.threshold(
					fgMask, 
					thresholdMat, 
					motionThreshold, 
					255, 
					opencv_imgproc.THRESH_BINARY);
			
			opencv_imgproc.medianBlur(thresholdMat, motionMask, 3);
			
			opencv_imgproc.morphologyEx(
					motionMask, 
					motionMask, 
					opencv_imgproc.MORPH_OPEN,
					morphKernel);
			
			opencv_imgproc.morphologyEx(
					motionMask, 
					motionMask, 
					opencv_imgproc.MORPH_CLOSE,
					morphKernel);
			
			return motionMask;
		}finally
		{
			thresholdMat.release();
		}
	}
	
	private List<MotionRegion> findMotionRegions(Mat motionMask)
	{
		if(motionMask==null || motionMask.empty())
			return List.of();
		
		Mat contourInput=motionMask.clone();
		MatVector contours=new MatVector();
		
		try
		{
			opencv_imgproc.findContours(
					contourInput, 
					contours, 
					opencv_imgproc.RETR_EXTERNAL, 
					opencv_imgproc.CHAIN_APPROX_SIMPLE);
			
			List<MotionRegion> regions=new ArrayList<>();
			
			for(long i=0; i<contours.size(); i++)
			{
				Mat contour=contours.get(i);
				
				try
				{
					double contourArea=opencv_imgproc.contourArea(contour);
					
					if(contourArea<minimumContourArea)
						continue;
					
					Rect rect=opencv_imgproc.boundingRect(contour);
					
					try
					{
						MotionRegion region=new MotionRegion(
								rect.x(),
								rect.y(),
								rect.width(),
								rect.height(),
								contourArea
								);
						
						regions.add(region);
					}
					finally
					{
						rect.close();
					}
				}
				finally
				{
					contour.release();
				}
			}
			
			return nonMaxSuppression.suppress(regions);
		}
		finally
		{
			contours.close();
			contourInput.release();
		}
	}
	
	private List<DetectionResult> buildDetectionResults(Frame frame, List<MotionRegion> regions)
	{
		if(regions==null || regions.isEmpty())
			return List.of();
		
		List<DetectionResult> results=new ArrayList<>();
		
		for(MotionRegion region : regions)
		{
			double confidence=calculateMotionConfidence(
					region.getContourArea(), 
					frame.getWidth(),
					frame.getHeight());
			
			Map<String, Object> metadata=new HashMap<>();
			metadata.put("frameId", frame.getFrameId());
			metadata.put("sourceName", frame.getSourceName());
			metadata.put("contourArea", region.getContourArea());
			metadata.put("minimumContourArea", minimumContourArea);
			metadata.put("motionThreshold", motionThreshold);
			
			DetectionResult result=new DetectionResult(
					DetectionType.MOTION_DETECTED,
					confidence,
					Instant.now(),
					region.getX(),
					region.getY(),
					region.getWidth(),
					region.getHeight(),
					metadata
					);
			
			results.add(result);
		}
		
		return results;
	}
	
	private double calculateMotionConfidence(double contourArea, int frameWidth, int frameHeight)
	{
		if(frameWidth<=0 || frameHeight<=0)
			return 0.0;
		
		double frameArea=frameWidth*frameHeight;
		double areaRatio=contourArea/frameArea;
		
		double confidence=areaRatio*25;
		
		if(confidence>1.0)
			return 1.0;
		
		if(confidence<0.0)
			return 0.0;
		
		return confidence;
	}
	
	private void validateFrame(Frame frame)
	{
		if(frame==null)
			throw new IllegalArgumentException("Frame cannot be null");
		
		if(frame.isEmpty())
			throw new IllegalArgumentException("Frame cannot be empty");
	}
	
	public void close()
	{
		if(morphKernel!=null)
			morphKernel.release();
		
		if(backSub!=null)
			backSub.close();
	}
}
