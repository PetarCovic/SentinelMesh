package com.sentinelmesh.edge.yolo;

import java.util.ArrayList;
import java.util.List;

public class YoloPostProcessor 
{
	private final double confidenceThreshold;
	private final double nmsThreshold;
	
	public YoloPostProcessor(double confidenceThreshold, double nmsThreshold)
	{
		if(confidenceThreshold<0 || confidenceThreshold>1)
			throw new IllegalArgumentException("ConfidenceThreshold must be between 0.0 and 1.0");
		
		if(nmsThreshold<0.0 || nmsThreshold>1.0)
			throw new IllegalArgumentException("nmsThreshold must be between 0.0 and 1.0");
		
		this.confidenceThreshold=confidenceThreshold;
		this.nmsThreshold=nmsThreshold;
	}
	
	public List<YoloPrediction> process(YoloOutput output)
	{
		if(output==null)
			throw new IllegalArgumentException("Output cannot be null");
		
		return applyNonMaxSuppression(filterByConfidence(output.getAllPredictions()));
	}
	
	private List<YoloPrediction> filterByConfidence(List<YoloPrediction> predictions)
	{
		if(predictions==null)
			throw new IllegalArgumentException("Predictions cannot be null");
		
		List<YoloPrediction> filteredPredictions=new ArrayList<>();
		
		for(YoloPrediction prediction : predictions)
		{
			if(prediction==null)
				throw new IllegalArgumentException("Predictions cannot contain null");
			
			if(prediction.isAboveThreshold(confidenceThreshold))
			{
				filteredPredictions.add(prediction);
			}
		}
		
		return List.copyOf(filteredPredictions);
	}
	
	private List<YoloPrediction> applyNonMaxSuppression(List<YoloPrediction> predictions)
	{
		if(predictions==null)
			throw new IllegalArgumentException("Predictions cannot be null");
		
		for(YoloPrediction prediction : predictions)
			if(prediction==null)
				throw new IllegalArgumentException("Predictions cannot contain null");
		
		List<YoloPrediction> sorted=sortByScoreDescending(predictions);
		List<YoloPrediction> keep=new ArrayList<>();
		
		for(YoloPrediction candidate : sorted)
		{
			boolean suppressed=false;
			
			for(YoloPrediction keptPrediction : keep)
			{
				if(isContained(keptPrediction.getBoundingBox(), candidate.getBoundingBox()))
				{
					suppressed=true;
					break;
				}
				
				double iou=calculateIOU(candidate.getBoundingBox(), keptPrediction.getBoundingBox());
				
				if(iou>nmsThreshold)
				{
					suppressed=true;
					break;
				}
			}
			
			if(!suppressed)
				keep.add(candidate);
		}
		
		return List.copyOf(keep);
	}
	
	private double calculateIOU(YoloBoundingBox a, YoloBoundingBox b)
	{
		if(a==null)
			throw new IllegalArgumentException("YoloBoundingBox a cannot be null");
		
		if(b==null)
			throw new IllegalArgumentException("YoloBoundingBox b cannot be null");
		
		double intersectionXMin=Math.max(a.getX(), b.getX());
		double intersectionXMax=Math.min(a.getRight(), b.getRight());
		double intersectionYMin=Math.max(a.getY(), b.getY());
		double intersectionYMax=Math.min(a.getBottom(), b.getBottom());
		
		double intersectionWidth=Math.max(0,  intersectionXMax-intersectionXMin);
		double intersectionHeight=Math.max(0, intersectionYMax-intersectionYMin);
		
		double intersectionArea=intersectionWidth*intersectionHeight;
		double unionArea=a.getArea()+b.getArea()-intersectionArea;
		
		if(unionArea<=0)
			return 0.0;
		
		return intersectionArea/unionArea;
	}
	
	private List<YoloPrediction> sortByScoreDescending(List<YoloPrediction> predictions)
	{
		if(predictions == null)
			throw new IllegalArgumentException("predictions cannot be null");
		
		List<YoloPrediction> sorted = new ArrayList<>(predictions);
		
		sorted.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
		
		return sorted;
	}
	
	private boolean isContained(YoloBoundingBox outer, YoloBoundingBox inner)
	{
		if(outer==null || inner==null)
			throw new IllegalArgumentException("YoloPrediction cannot be null");
		
		return inner.getX()>=outer.getX()
				&& inner.getRight()<=outer.getRight()
				&& inner.getY()>=outer.getY()
				&& inner.getBottom()<=outer.getBottom();
	}
}
