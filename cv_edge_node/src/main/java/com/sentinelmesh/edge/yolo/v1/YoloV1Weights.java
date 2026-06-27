package com.sentinelmesh.edge.yolo.v1;

import java.util.List;

public class YoloV1Weights 
{
	private final List<YoloV1ConvLayerWeights> convLayers;
	
	private final float[][] hiddenWeights;
	private final float[] hiddenBiases;
	
	private final float[][] outputWeights;
	private final float[] outputBiases;
	
	public YoloV1Weights(
			List<YoloV1ConvLayerWeights>convLayers,
			float[][] hiddenWeights,
			float[] hiddenBiases,
			float[][] outputWeights,
			float[] outputBiases
			)
	{		
		this.convLayers=List.copyOf(convLayers);
		this.hiddenWeights=hiddenWeights;
		this.hiddenBiases=hiddenBiases;
		this.outputWeights=outputWeights;
		this.outputBiases=outputBiases;
		
		validate();
	}
	
	public List<YoloV1ConvLayerWeights> getConvLayers()
	{
		return convLayers;
	}
	
	public float[][] getHiddenWeights()
	{
		return hiddenWeights;
	}
	
	public float[] getHiddenBiases()
	{
		return hiddenBiases;
	}
	
	public float[][] getOutputWeights()
	{
		return outputWeights;
	}
	
	public float[] getOutputBiases()
	{
		return outputBiases;
	}
	
	private void validate()
	{
		if(convLayers==null || convLayers.isEmpty())
			throw new IllegalArgumentException("ConvLayers cannot be null or empty");
		
		for(int i = 0; i < convLayers.size(); i++)
		{
			if(convLayers.get(i) == null)
				throw new IllegalArgumentException("ConvLayers cannot contain "
						+ "null layer at index " + i);
		}
		
		validateFullyConnectedWeights(hiddenWeights, hiddenBiases, "Hidden");
		validateFullyConnectedWeights(outputWeights, outputBiases, "Output");
	}

	private void validateFullyConnectedWeights(float[][] weights, float[] biases, String label)
	{
		if(weights == null || weights.length == 0)
			throw new IllegalArgumentException(label + " weights cannot be null or empty");
		
		if(biases == null || biases.length == 0)
			throw new IllegalArgumentException(label + " biases cannot be null or empty");
		
		if(weights.length != biases.length)
			throw new IllegalArgumentException(label + " weights length must match biases length");
		
		if(weights[0] == null || weights[0].length == 0)
			throw new IllegalArgumentException(label + " weights[0] cannot be null or empty");
		
		int inputSize = weights[0].length;
		
		for(int outputIndex = 0; outputIndex < weights.length; outputIndex++)
		{
			if(weights[outputIndex] == null)
				throw new IllegalArgumentException(label + " weights cannot contain null rows");
			
			if(weights[outputIndex].length != inputSize)
			{
				throw new IllegalArgumentException(
						label + " weights[" + outputIndex + "] must have inputSize " + inputSize
				);
			}
		}
	}
}