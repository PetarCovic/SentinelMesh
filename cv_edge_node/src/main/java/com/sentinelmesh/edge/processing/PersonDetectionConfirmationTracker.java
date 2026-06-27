package com.sentinelmesh.edge.processing;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import com.sentinelmesh.edge.detection.DetectionResult;
import com.sentinelmesh.edge.detection.DetectionType;

public class PersonDetectionConfirmationTracker
{
	private final int requiredPositiveCount;
	private final int windowSize;
	private final Deque<Boolean> recentPersonDetections;
	
	public PersonDetectionConfirmationTracker()
	{
		this(2, 3);
	}
	
	public PersonDetectionConfirmationTracker(int requiredPositiveCount, int windowSize)
	{
		if(windowSize <= 0)
			throw new IllegalArgumentException("Window size must be positive");
		
		if(requiredPositiveCount <= 0)
			throw new IllegalArgumentException("Required positive count must be positive");
		
		if(requiredPositiveCount > windowSize)
			throw new IllegalArgumentException("Required positive count cannot exceed window size");
		
		this.requiredPositiveCount = requiredPositiveCount;
		this.windowSize = windowSize;
		this.recentPersonDetections = new ArrayDeque<>();
	}
	
	public boolean recordAndCheck(List<DetectionResult> detections)
	{
		boolean hasPersonDetection = containsPersonDetection(detections);
		
		recentPersonDetections.addLast(hasPersonDetection);
		
		while(recentPersonDetections.size() > windowSize)
			recentPersonDetections.removeFirst();
		
		return isConfirmed();
	}
	
	public void reset()
	{
		recentPersonDetections.clear();
	}
	
	private boolean isConfirmed()
	{
		int positiveCount = 0;
		
		for(Boolean detected : recentPersonDetections)
		{
			if(Boolean.TRUE.equals(detected))
				positiveCount++;
		}
		
		return recentPersonDetections.size() == windowSize
				&& positiveCount >= requiredPositiveCount;
	}
	
	private boolean containsPersonDetection(List<DetectionResult> detections)
	{
		if(detections == null || detections.isEmpty())
			return false;
		
		for(DetectionResult detection : detections)
		{
			if(detection != null && detection.getType() == DetectionType.PERSON_DETECTED)
				return true;
		}
		
		return false;
	}
}