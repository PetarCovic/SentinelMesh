package com.sentinelmesh.edge.yolo.v1;

import com.sentinelmesh.edge.cnn.Padding;

public class YoloV1ConvLayerWeights 
{
	private final float[][][][] filters;
	private final float[] biases;
	private final int stride;
	private final Padding padding;
	private final boolean applyLeakyReluAfter;
	private final boolean applyMaxPoolAfter;
	private final int maxPoolSize;
	private final int maxPoolStride;
	
	public YoloV1ConvLayerWeights(
			float[][][][] filters,
			float[] biases,
			int stride,
			Padding padding,
			boolean applyLeakyReluAfter,
			boolean applyMaxPoolAfter,
			int maxPoolSize,
			int maxPoolStride
			)
	{		
		this.filters=filters;
		this.biases=biases;
		this.stride=stride;
		this.padding=padding;
		this.applyLeakyReluAfter=applyLeakyReluAfter;
		this.applyMaxPoolAfter=applyMaxPoolAfter;
		this.maxPoolSize=maxPoolSize;
		this.maxPoolStride=maxPoolStride;
		
		validate();
	}
	
	public float[][][][] getFilters()
	{
		return filters;
	}
	
	public float[] getBiases()
	{
		return biases;
	}
	
	public int getStride()
	{
		return stride;
	}
	
	public Padding getPadding()
	{
		return padding;
	}
	
	public boolean isApplyLeakyReluAfter()
	{
		return applyLeakyReluAfter;
	}
	
	public boolean isApplyMaxPoolAfter()
	{
		return applyMaxPoolAfter;
	}
	
	public int getMaxPoolSize()
	{
		return maxPoolSize;
	}
	
	public int getMaxPoolStride()
	{
		return maxPoolStride;
	}
	
	private void validate()
	{
		if(filters == null)
			throw new IllegalArgumentException("Filters cannot be null");
		
		if(filters.length == 0)
			throw new IllegalArgumentException("Filters cannot be empty");
		
		if(biases == null)
			throw new IllegalArgumentException("Biases cannot be null");
		
		if(biases.length != filters.length)
			throw new IllegalArgumentException("Bias count must match filter count");
		
		if(stride <= 0)
			throw new IllegalArgumentException("Stride must be positive");
		
		if(padding == null)
			throw new IllegalArgumentException("Padding cannot be null");
		
		validateFilters();
		validatePooling();
	}
	
	private void validateFilters()
	{
		int kernelHeight = -1;
		int kernelWidth = -1;
		int inputChannels = -1;
		
		for(int filterIndex = 0; filterIndex < filters.length; filterIndex++)
		{
			float[][][] filter = filters[filterIndex];
			
			if(filter == null)
				throw new IllegalArgumentException("Filter cannot be null at index " + filterIndex);
			
			if(filter.length == 0)
				throw new IllegalArgumentException("Filter height must be positive at index " + filterIndex);
			
			if(kernelHeight == -1)
				kernelHeight = filter.length;
			else if(filter.length != kernelHeight)
				throw new IllegalArgumentException("All filters must have the same kernel height");
			
			for(int y = 0; y < filter.length; y++)
			{
				if(filter[y] == null)
					throw new IllegalArgumentException("Filter row cannot be null at filter " + filterIndex + ", row " + y);
				
				if(filter[y].length == 0)
					throw new IllegalArgumentException("Filter width must be positive at filter " + filterIndex + ", row " + y);
				
				if(kernelWidth == -1)
					kernelWidth = filter[y].length;
				else if(filter[y].length != kernelWidth)
					throw new IllegalArgumentException("All filters must have the same kernel width");
				
				for(int x = 0; x < filter[y].length; x++)
				{
					if(filter[y][x] == null)
						throw new IllegalArgumentException("Filter channel array cannot be null at filter " + filterIndex + ", y " + y + ", x " + x);
					
					if(filter[y][x].length == 0)
						throw new IllegalArgumentException("Input channels must be positive at filter " + filterIndex + ", y " + y + ", x " + x);
					
					if(inputChannels == -1)
						inputChannels = filter[y][x].length;
					else if(filter[y][x].length != inputChannels)
						throw new IllegalArgumentException("All filters must have the same input channel count");
				}
			}
		}
	}
	
	private void validatePooling()
	{
		if(applyMaxPoolAfter)
		{
			if(maxPoolSize <= 0)
				throw new IllegalArgumentException("Max pool size must be positive when max pooling is enabled");
			
			if(maxPoolStride <= 0)
				throw new IllegalArgumentException("Max pool stride must be positive when max pooling is enabled");
		}
		else
		{
			if(maxPoolSize < 0)
				throw new IllegalArgumentException("Max pool size cannot be negative");
			
			if(maxPoolStride < 0)
				throw new IllegalArgumentException("Max pool stride cannot be negative");
		}
	}
}
