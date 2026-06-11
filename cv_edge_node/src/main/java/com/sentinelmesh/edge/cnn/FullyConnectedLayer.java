package com.sentinelmesh.edge.cnn;

public class FullyConnectedLayer implements Layer<Tensor1D, Tensor1D>
{
	private final int inputSize;
	private final int outputSize;
	private final float[][] weights;
	private final float[] biases;
	
	public FullyConnectedLayer(float[][] weights, float[] biases)
	{
		if(weights==null || weights.length==0)
			throw new IllegalArgumentException("Weights cannot be null or empty");
		
		if(biases==null || biases.length==0)
			throw new IllegalArgumentException("Biases cannot be null or empty");
		
		if(weights.length!=biases.length)
			throw new IllegalArgumentException("Weights length must equal biases length");
		
		if(weights[0] == null || weights[0].length == 0)
			throw new IllegalArgumentException("Weights[0] cannot be null or empty");
		
		this.inputSize=weights[0].length;
		this.outputSize=weights.length;
		
		if(inputSize<=0)
			throw new IllegalArgumentException("InputSize must be greater than 0");
		
		for(int i=0; i<weights.length; i++)
		{
			if(weights[i]==null)
				throw new IllegalArgumentException("weights cannot contain null");
		
			if(weights[i].length!=inputSize)
				throw new IllegalArgumentException("Weights["+i+"] must be equal to "+inputSize);
		}
		
		this.weights=weights;
		this.biases=biases;
	}

	@Override
	public Tensor1D forward(Tensor1D input)
	{
		if(input==null)
			throw new IllegalArgumentException("Input cannot be null");
		
		if(input.getLength()!=inputSize)
			throw new IllegalArgumentException("Input length must be equal to input size");
		
		Tensor1D output=new Tensor1D(outputSize);
		
		float sum;
		for(int outputIndex=0; outputIndex<outputSize; outputIndex++)
		{
			sum=biases[outputIndex];
			
			for(int inputIndex=0; inputIndex<inputSize; inputIndex++)
			{
				sum+=input.get(inputIndex)*weights[outputIndex][inputIndex];
			}
			
			output.set(outputIndex, sum);
		}
		
		return output;
	}

	public int getInputSize()
	{
		return inputSize;
	}
	
	public int getOutputSize()
	{
		return outputSize;
	}
}