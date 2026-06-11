package com.sentinelmesh.edge.cnn;

public class LeakyReluLayer implements Layer<Tensor3D, Tensor3D>
{
	private final ActivationFunction activationFunction;
	
	public LeakyReluLayer()
	{
		this.activationFunction=new LeakyReluActivation();
	}
	
	public LeakyReluLayer(ActivationFunction activationFunction)
	{
		if(activationFunction==null)
			throw new IllegalArgumentException("ActivationFunction cannot be null");
		
		this.activationFunction=activationFunction;
	}

	@Override
	public Tensor3D forward(Tensor3D input) 
	{
		if(input==null)
			throw new IllegalArgumentException("Input cannot be null");
		
		int height=input.getHeight();
		int width=input.getWidth();
		int channels=input.getChannels();
		
		Tensor3D output=new Tensor3D(height, width, channels);
		
		for(int y=0; y<height; y++)
		{
			for(int x=0; x<width; x++)
			{
				for(int c=0; c<channels; c++)
				{
					output.set(y, x, c, activationFunction.apply(input.get(y, x, c)));
				}
			}
		}
		
		return output;
	}
}
