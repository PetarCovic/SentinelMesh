package com.sentinelmesh.edge.yolo;

import com.sentinelmesh.edge.detection.DetectionType;

public class YoloPrediction 
{
	private final YoloBoundingBox boundingBox;
	private final double objectness;
	private final double classProbability;
	private final double score;
	private final String className;
	
	public YoloPrediction(
			YoloBoundingBox boundingBox,
			double objectness,
			double classProbability,
			String className
			)
	{
		if(boundingBox==null)
			throw new IllegalArgumentException("BoundingBox cannot be null");
		
		if(objectness<0 || objectness>1)
			throw new IllegalArgumentException("Objectness must be between 0.0 and 1.0");
		
		if(classProbability<0 || classProbability>1)
			throw new IllegalArgumentException("ClassProbability must be between 0.0 and 1.0");
		
		if(className==null || className.isBlank())
			throw new IllegalArgumentException("ClassName cannot be null or empty");
		
		this.boundingBox=boundingBox;
		this.objectness=objectness;
		this.classProbability=classProbability;
		this.score=objectness*classProbability;
		this.className=className;
	}
	
	public YoloBoundingBox getBoundingBox()
	{
		return boundingBox;
	}
	
	public double getObjectness()
	{
		return objectness;
	}
	
	public double getClassProbability()
	{
		return classProbability;
	}
	
	public double getScore()
	{
		return score;
	}
	
	public String getClassName()
	{
		return className;
	}
	
	public double getX()
	{
		return boundingBox.getX();
	}
	
	public double getY()
	{
		return boundingBox.getY();
	}
	
	public double getWidth()
	{
		return boundingBox.getWidth();
	}
	
	public double getHeight()
	{
		return boundingBox.getHeight();
	}
	
	public double getRight()
	{
		return boundingBox.getRight();
	}
	
	public double getBottom()
	{
		return boundingBox.getBottom();
	}
	
	public boolean isAboveThreshold(double threshold)
	{
		if(threshold<0 || threshold>1)
			throw new IllegalArgumentException("Threshold must be between 0.0 and 1.0");
		
		return score>=threshold;
	}
	
	@Override
	public String toString()
	{
		return "YoloPrediction{"
				+"className='"+className+'\''
				+", objectness="+objectness
				+", classProbability="+classProbability
				+", score="+score
				+", boundingBox=("
				+boundingBox.getX() + ", " 
				+boundingBox.getY() + ", " 
				+boundingBox.getWidth() + ", " 
				+boundingBox.getHeight() + ")}";
	}
}
