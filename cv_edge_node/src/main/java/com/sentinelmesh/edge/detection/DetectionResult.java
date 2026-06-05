package com.sentinelmesh.edge.detection;

import java.time.Instant;
import java.util.Map;

public class DetectionResult 
{
	private final DetectionType type;
	private final double confidence;
	private final Instant timestamp;
	private final int boundingBoxX;
	private final int boundingBoxY;
	private final int boundingBoxWidth;
	private final int boundingBoxHeight;
	private final Map<String, Object> metadata;
	
	public DetectionResult(
			DetectionType type,
			double confidence,
			Instant timestamp,
			int boundingBoxX,
			int boundingBoxY,
			int boundingBoxWidth,
			int boundingBoxHeight,
			Map<String, Object> metadata
			)
	{
		if(type==null)
			throw new IllegalArgumentException("Detection Type cannot be null");
		
		if(timestamp==null)
			throw new IllegalArgumentException("Timestamp cannot be null");
		
		if(confidence>1 || confidence<0)
			throw new IllegalArgumentException("Confidence must be between 0.0 and 1.0");
		
		if(boundingBoxX<-1)
			throw new IllegalArgumentException("Bounding box X must be >=-1");
		
		if(boundingBoxY<-1)
			throw new IllegalArgumentException("Bounding box Y must be >=-1");
		
		if(boundingBoxWidth<0)
			throw new IllegalArgumentException("Bounding box width cannot be negative");
		
		if(boundingBoxHeight<0)
			throw new IllegalArgumentException("Bounding box height cannot be negative");
		
		this.type=type;
		this.confidence=confidence;
		this.timestamp=timestamp;
		this.boundingBoxX=boundingBoxX;
		this.boundingBoxY=boundingBoxY;
		this.boundingBoxWidth=boundingBoxWidth;
		this.boundingBoxHeight=boundingBoxHeight;
		
		if(metadata==null)
			this.metadata=Map.of();
		else
			this.metadata=Map.copyOf(metadata);
	}
	
	public DetectionType getType()
	{
		return type;
	}
	
	public double getConfidence()
	{
		return confidence;
	}
	
	public Instant getTimestamp()
	{
		return timestamp;
	}
	
	public int getBoundingBoxX()
	{
		return boundingBoxX;
	}
	
	public int getBoundingBoxY()
	{
		return boundingBoxY;
	}
	
	public int getBoundingBoxWidth()
	{
		return boundingBoxWidth;
	}
	
	public int getBoundingBoxHeight()
	{
		return boundingBoxHeight;
	}
	
	public int getBoundingBoxRight()
	{
		return boundingBoxX+boundingBoxWidth;
	}
	
	public int getBoundingBoxBottom()
	{
		return boundingBoxY+boundingBoxHeight;
	}
	
	public String getBoundingBoxString()
	{
		if(!hasBoundingBox())
			return "None";
		
		return "("+boundingBoxX+", "+boundingBoxY+"), ("
				+getBoundingBoxRight()+", "+boundingBoxY+"), ("
				+boundingBoxX+", "+getBoundingBoxBottom()+"), ("
				+getBoundingBoxRight()+", "+getBoundingBoxBottom()+")";
	}
	
	public Map<String, Object> getMetadata()
	{
		return metadata;
	}
	
	public String getMetadataString()
	{
		return "Frame Id: "+metadata.get("frameId")
				+"\nSource Name: "+metadata.get("sourceName")
				+"\nContour Area: "+metadata.get("contourArea")
				+"\nMinimum Contour Area: "+metadata.get("minimumContourArea")
				+"\nMotion Threshold: "+metadata.get("motionThreshold");
	}
	
	public boolean hasBoundingBox()
	{		
		return boundingBoxX >= 0
		        && boundingBoxY >= 0
		        && boundingBoxWidth > 0
		        && boundingBoxHeight > 0;
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb=new StringBuilder();
		
		sb.append("Detection Type: "+type);
		sb.append("\nConfidence: "+confidence);
		sb.append("\nTimestamp: "+timestamp);
		sb.append("\nBounding Box: "+getBoundingBoxString());
		sb.append("\nMetadata\n");
		
		for (Map.Entry<String, Object> entry : metadata.entrySet()) 
			sb.append(entry.getKey()+": "+entry.getValue()+"\n");
		
		return sb.toString();
	}
}