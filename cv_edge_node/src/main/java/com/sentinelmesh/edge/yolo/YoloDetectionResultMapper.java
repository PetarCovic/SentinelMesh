package com.sentinelmesh.edge.yolo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sentinelmesh.edge.camera.Frame;
import com.sentinelmesh.edge.detection.DetectionResult;
import com.sentinelmesh.edge.detection.DetectionType;

public class YoloDetectionResultMapper 
{
	public List<DetectionResult> toDetectionResults(List<YoloPrediction> predictions, Frame frame)
	{
		if(predictions==null)
			throw new IllegalArgumentException("Predictions cannot be null");
		
		if(frame==null || frame.isEmpty())
			throw new IllegalArgumentException("Frame cannot be null or empty");
		
		List<DetectionResult> results=new ArrayList<>();
		
		for(YoloPrediction prediction : predictions)
		{
			if(prediction==null)
				throw new IllegalArgumentException("Predictions cannot contain null");
			
			DetectionType type=mapDetectionType(prediction.getClassName());
			double confidence=prediction.getScore();
			Instant timestamp=Instant.now();
			int boundingBoxX=(int)Math.round(prediction.getX());
			int boundingBoxY=(int)Math.round(prediction.getY());
			int boundingBoxWidth=(int)Math.round(prediction.getWidth());
			int boundingBoxHeight=(int)Math.round(prediction.getHeight());
			Map<String, Object> metadata=new HashMap<>();
			
			metadata.put("detector", "YOLOv1");
			metadata.put("className", prediction.getClassName());
			metadata.put("objectness", prediction.getObjectness());
			metadata.put("classProbability", prediction.getClassProbability());
			metadata.put("score", prediction.getScore());
			metadata.put("sourceName", frame.getSourceName());
			metadata.put("frameId", frame.getFrameId());
			
			if(boundingBoxX<0)
				throw new IllegalArgumentException("BoundingBoxX cannot be negative");
			
			if(boundingBoxY<0)
				throw new IllegalArgumentException("BoundingBoxY cannot be negative");
			
			if(boundingBoxWidth<=0 || boundingBoxHeight<=0)
				continue;
			
			results.add(new DetectionResult(
					type,
					confidence,
					timestamp,
					boundingBoxX,
					boundingBoxY,
					boundingBoxWidth,
					boundingBoxHeight,
					metadata
					));
		}
		
		return List.copyOf(results);
	}
	
	private DetectionType mapDetectionType(String className)
	{
		if(className==null || className.isBlank())
			throw new IllegalArgumentException("ClassName cannot be null or blank");
		
		if(className.equalsIgnoreCase("person"))
			return DetectionType.PERSON_DETECTED;
		
		throw new IllegalArgumentException("Unsupported YOLO class name: "+className);
	}
}
