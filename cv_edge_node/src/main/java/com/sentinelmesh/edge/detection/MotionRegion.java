package com.sentinelmesh.edge.detection;

public class MotionRegion
{
	private final int x;
	private final int y;
	private final int width;
	private final int height;
	private final double contourArea;
	
	public MotionRegion(int x, int y, int width, int height, double contourArea)
	{
		if(x<0)
			throw new IllegalArgumentException("X must be greater than or equal to 0");
		
		if(y<0)
			throw new IllegalArgumentException("Y must be greater than or equal to 0");
		
		if(width<=0)
			throw new IllegalArgumentException("Width must be greater than 0");
		
		if(height<=0)
			throw new IllegalArgumentException("Height must be greater than 0");
		
		if(contourArea<=0)
			throw new IllegalArgumentException("Contour area must be greater than 0");
		
		this.x=x;
		this.y=y;
		this.width=width;
		this.height=height;
		this.contourArea=contourArea;
	}
	
	public double getContourArea()
	{
		return contourArea;
	}
	
	public int getX()
	{
		return x;
	}
	
	public int getY()
	{
		return y;
	}
	
	public int getWidth()
	{
		return width;
	}
	
	public int getHeight()
	{
		return height;
	}
	
	public int getRight()
	{
		return getX()+getWidth();
	}
	
	public int getBottom()
	{
		return getY()+getHeight();
	}
}
