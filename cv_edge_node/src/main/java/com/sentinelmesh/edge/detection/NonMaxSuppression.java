package com.sentinelmesh.edge.detection;

import java.util.ArrayList;
import java.util.List;

public class NonMaxSuppression 
{
	private final double iouThreshold=.3;
	
	public List<MotionRegion> suppress(List<MotionRegion> regions)
	{
		return suppress(regions, iouThreshold);
	}
	
	public List<MotionRegion> suppress(List<MotionRegion> regions, double iouThreshold)
	{
		if(regions==null)
			throw new IllegalArgumentException("Regions cannot be null");
		
		if(regions.isEmpty())
			return List.of();
		
		if(iouThreshold<0 || iouThreshold>1)
			throw new IllegalArgumentException("Threshold must be between 0 and 1");
		
		List<MotionRegion> sorted=sortByContourAreaDescending(regions);
		List<MotionRegion> keep=new ArrayList<>();
		
		for(MotionRegion candidate : sorted)
		{
			boolean suppressed=false;
			
			for(MotionRegion keptRegion : keep)
			{
				if(isContained(keptRegion, candidate))
				{
					suppressed=true;
					break;
				}
				
				double iou=calculateIoU(candidate, keptRegion);
				
				if(iou>iouThreshold)
				{
					suppressed=true;
					break;
				}
			}
			
			if(!suppressed)
				keep.add(candidate);
		}
		
		return keep;
	}
	
	public double calculateIoU(MotionRegion a, MotionRegion b)
	{
		if(a==null || b==null)
			throw new IllegalArgumentException("Motion Regions cannot be null");
		
		int intersectionLeft=Math.max(a.getX(), b.getX());
		int intersectionRight=Math.min(a.getRight(), b.getRight());
		int intersectionTop=Math.max(a.getY(), b.getY());
		int intersectionBottom=Math.min(a.getBottom(), b.getBottom());
		
		int intersectionWidth=Math.max(0, intersectionRight-intersectionLeft);
		int intersectionHeight=Math.max(0, intersectionBottom-intersectionTop);
		
		double intersectionArea=intersectionWidth*intersectionHeight;
		
		double areaA=a.getWidth()*a.getHeight();
		double areaB=b.getWidth()*b.getHeight();
		
		double unionArea=areaA+areaB-intersectionArea;
		
		if(unionArea<=0)
			return 0.0;
		
		return intersectionArea/unionArea;
	}
	
	public boolean isContained(MotionRegion outer, MotionRegion inner)
	{
		if(outer==null || inner==null)
			throw new IllegalArgumentException("Motion Regions cannot be null");
		
		return inner.getX()>=outer.getX()
				&& inner.getRight()<=outer.getRight()
				&& inner.getY()>=outer.getY()
				&& inner.getBottom()<=outer.getBottom();
	}
	
	private List<MotionRegion> sortByContourAreaDescending(List<MotionRegion> regions)
	{
		if(regions == null)
			throw new IllegalArgumentException("Regions cannot be null");
		
		List<MotionRegion> sorted = new ArrayList<>(regions);
		
		sorted.sort((a, b) -> Double.compare(b.getContourArea(), a.getContourArea()));
		
		return sorted;
	}
}
