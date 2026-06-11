package com.sentinelmesh.edge.yolo;

public class YoloBoundingBox 
{
	private final double x;
	private final double y;
	private final double width;
	private final double height;
	
	public YoloBoundingBox(double x, double y, double width, double height)
	{
		if(x<0)
			throw new IllegalArgumentException("X cannot be negative");
		
		if(y<0)
			throw new IllegalArgumentException("Y cannot be negative");
		
		if(width<=0)
			throw new IllegalArgumentException("Width must be greater than 0");
		
		if(height<=0)
			throw new IllegalArgumentException("Height must be greater than 0");
		
		this.x=x;
		this.y=y;
		this.width=width;
		this.height=height;
	}
	
	public double getX()
	{
		return x;
	}
	
	public double getY()
	{
		return y;
	}
	
	public double getWidth()
	{
		return width;
	}
	
	public double getHeight()
	{
		return height;
	}
	
	public double getRight()
	{
		return x+width;
	}
	
	public double getBottom()
	{
		return y+height;
	}
	
	public double getCenterX()
	{
		return x+width/2;
	}
	
	public double getCenterY()
	{
		return y+height/2;
	}
	
	public double getArea()
	{
		return width*height;
	}
	
	public static YoloBoundingBox fromCenter(
			double centerX,
			double centerY,
			double width,
			double height
			)
	{
		double x=centerX-width/2;
		double y=centerY-width/2;
		
		return new YoloBoundingBox(x, y, width, height);
	}
}
