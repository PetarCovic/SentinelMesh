package com.sentinelmesh.edge.detection;

import java.util.ArrayList;
import java.util.List;

import com.sentinelmesh.edge.camera.Frame;

public class DetectionPipeline implements Detector
{
	private final List<Detector> detectors;
	
	public DetectionPipeline(List<Detector> detectors)
	{
		if(detectors==null || detectors.isEmpty())
			throw new IllegalArgumentException("Detectors cannot be null or empty");
		
		for(Detector detector : detectors)
		{
		    if(detector == null)
		        throw new IllegalArgumentException("Detectors cannot contain null");
		}
		
		this.detectors=List.copyOf(detectors);
	}
	
	@Override
	public List<DetectionResult> detect(Frame frame)
	{
		if(frame==null || frame.isEmpty())
			throw new IllegalArgumentException("Frame cannot be null or empty");
		
		List<DetectionResult> results=new ArrayList<>();
		
		for(Detector detector : detectors)
		{
			List<DetectionResult> detections=detector.detect(frame);
			
			if(detections!=null && !detections.isEmpty())
				results.addAll(detections);
		}
		
		return results;
	}
}
