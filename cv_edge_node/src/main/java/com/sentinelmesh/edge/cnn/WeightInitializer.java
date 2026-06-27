package com.sentinelmesh.edge.cnn;

import java.util.Random;

public final class WeightInitializer 
{
	private WeightInitializer()
	{
		
	}
	
	public static float[][][][] createConvFilters(
			int filterCount, 
			int kernelHeight, 
			int kernelWidth,
			int inputChannels,
			float min,
			float max
			)
	{
		if(filterCount<=0)
			throw new IllegalArgumentException("FilterCount must be greater than 0");
		
		if(kernelHeight<=0)
			throw new IllegalArgumentException("KernelHeight must be greater than 0");
		
		if(kernelWidth<=0)
			throw new IllegalArgumentException("KernelWidth must be greater than 0");
		
		if(inputChannels<=0)
			throw new IllegalArgumentException("InputChannels must be greater than 0");
		
		if(min>max)
			throw new IllegalArgumentException("Min must be less than or equal to max");
		
		Random random=new Random();
		float[][][][] filters=new float[filterCount][kernelHeight][kernelWidth][inputChannels];
		
		for(int i=0; i<filterCount; i++)
		{
			for(int j=0; j<kernelHeight; j++)
			{
				for(int k=0; k<kernelWidth; k++)
				{
					for(int l=0; l<inputChannels; l++)
					{
						filters[i][j][k][l]=min+random.nextFloat()*(max-min);
					}
				}
			}
		}
		
		return filters;
	}
	
	public static float[][] createFullyConnectedWeights(
			int outputSize, 
			int inputSize, 
			float min, 
			float max
			)
	{
		if(outputSize<=0)
			throw new IllegalArgumentException("OutputSize must be greater than 0");
		
		if(inputSize<=0)
			throw new IllegalArgumentException("InputSize must be greater than 0");
		
		if(min>max)
			throw new IllegalArgumentException("Min must be less than or equal to max");
		
		Random random=new Random();
		float[][] weights=new float[outputSize][inputSize];
		
		for(int i=0; i<outputSize; i++)
		{
			for(int j=0; j<inputSize; j++)
			{
				weights[i][j]=min+random.nextFloat()*(max-min);
			}
		}
		
		return weights;
	}
	
	public static float[] createBiases(int size, float value)
	{
		if(size<=0)
			throw new IllegalArgumentException("Size must be greater than 0");
		
		float[] biases=new float[size];
		for(int i=0; i<size; i++)
			biases[i]=value;
		
		return biases;
	}
}
