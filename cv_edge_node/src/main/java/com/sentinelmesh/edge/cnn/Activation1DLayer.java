package com.sentinelmesh.edge.cnn;

public class Activation1DLayer implements Layer<Tensor1D, Tensor1D>
{
	private final ActivationFunction activationFunction;
	
	public Activation1DLayer()
	{
		this(new LeakyReluActivation());
	}
	
	public Activation1DLayer(ActivationFunction activationFunction)
	{
		if(activationFunction==null)
			throw new IllegalArgumentException("Activation Function cannot be null");
		
		this.activationFunction=activationFunction;
	}
	
	@Override
	public Tensor1D forward(Tensor1D input) 
	{
		if(input==null)
			throw new IllegalArgumentException("Input cannot be null");
		
		Tensor1D output=new Tensor1D(input.getLength());
		
		for(int i=0; i<input.getLength(); i++)
		{
			output.set(i, activationFunction.apply(input.get(i)));
			
		}
		
		return output;
	}

}
