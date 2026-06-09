package com.sentinelmesh.edge.detection;

import java.util.List;

import com.sentinelmesh.edge.camera.Frame;

public class PersonDetector implements Detector
{
	@Override
	public List<DetectionResult> detect(Frame frame)
	{
		if(frame==null || frame.isEmpty())
			throw new IllegalArgumentException("Frame cannot be null or empty");
		
		return List.of();
	}
}
