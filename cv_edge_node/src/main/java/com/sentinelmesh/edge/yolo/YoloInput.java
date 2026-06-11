package com.sentinelmesh.edge.yolo;

public class YoloInput 
{
	private final float[][][] imageData;
	private final int originalWidth;
	private final int originalHeight;
	private final int inputWidth;
	private final int inputHeight;
	
	//Layout: imageData[y][x][channel], Channel order RGB, normalized 0.0 to 1.0
	public YoloInput(
			float[][][] imageData,
			int originalWidth,
			int originalHeight,
			int inputWidth,
			int inputHeight
			)
	{
		if(imageData==null)
			throw new IllegalArgumentException("imageData cannot be null");
		
		if(imageData[0]==null)
			throw new IllegalArgumentException("imageData[0] cannot be null");
		
		if(imageData[0][0]==null)
			throw new IllegalArgumentException("imageData[0][0] cannot be null");
		
		if(originalWidth<=0)
			throw new IllegalArgumentException("Original width must be greater than 0");
		
		if(originalHeight<=0)
			throw new IllegalArgumentException("Original height must be greater than 0");
		
		if(inputWidth<=0)
			throw new IllegalArgumentException("Input width must be greater than 0");
		
		if(inputHeight<=0)
			throw new IllegalArgumentException("Input height must be greater than 0");
		
		if(imageData.length!=inputHeight)
			throw new IllegalArgumentException("ImageData length must be equal to input height");
		
		if(imageData[0].length!=inputWidth)
			throw new IllegalArgumentException("ImageData[0] length must be equal to input width");
		
		if(imageData[0][0].length!=3)
			throw new IllegalArgumentException("ImageData[0][0] length must be equal 3 channels");
		
		this.imageData=imageData;
		this.originalWidth=originalWidth;
		this.originalHeight=originalHeight;
		this.inputWidth=inputWidth;
		this.inputHeight=inputHeight;
	}
	
	public float[][][] getImageData()
	{
		return imageData;
	}
	
	public int getOriginalWidth()
	{
		return originalWidth;
	}
	
	public int getOriginalHeight()
	{
		return originalHeight;
	}
	
	public int getInputWidth()
	{
		return inputWidth;
	}
	
	public int getInputHeight()
	{
		return inputHeight;
	}
}