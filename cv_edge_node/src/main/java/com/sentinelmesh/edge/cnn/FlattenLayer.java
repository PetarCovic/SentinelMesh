package com.sentinelmesh.edge.cnn;

public class FlattenLayer implements Layer<Tensor3D, Tensor1D>
{	
	@Override
	public Tensor1D forward(Tensor3D input) 
	{
		if(input==null)
			throw new IllegalArgumentException("Input cannot be null");
		
		Tensor1D output=new Tensor1D(input.getHeight()*input.getWidth()*input.getChannels());
		
		int index=0;
		for(int y=0; y<input.getHeight(); y++)
		{
			for(int x=0; x<input.getWidth(); x++)
			{
				for(int c=0; c<input.getChannels(); c++)
				{
					output.set(index, input.get(y, x, c));
					index++;
				}
			}
		}
		
		return output;
	}
}
