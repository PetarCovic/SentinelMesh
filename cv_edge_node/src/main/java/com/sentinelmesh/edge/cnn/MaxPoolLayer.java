package com.sentinelmesh.edge.cnn;

public class MaxPoolLayer implements Layer<Tensor3D, Tensor3D>
{
	private final int poolSize;
	private final int stride;
	
	public MaxPoolLayer()
	{
		this(2, 2);
	}
	
	public MaxPoolLayer(int poolSize, int stride)
	{
		if(poolSize<=0)
			throw new IllegalArgumentException("PoolSize must be greater than 0");
		
		if(stride<=0)
			throw new IllegalArgumentException("Stride must be greater than 0");
		
		this.poolSize=poolSize;
		this.stride=stride;
	}

	@Override
	public Tensor3D forward(Tensor3D input) 
	{
		if(input==null)
			throw new IllegalArgumentException("Input cannot be null");
		
		if(input.getHeight()<poolSize)
			throw new IllegalArgumentException("Input height must be at least poolSize");
		
		if(input.getWidth()<poolSize)
			throw new IllegalArgumentException("Input width must be at least poolSize");
		
		Tensor3D output=new Tensor3D(
				((input.getHeight()-poolSize)/stride)+1, 
				((input.getWidth()-poolSize)/stride)+1, 
				input.getChannels());
		
		for(int y=0; y<output.getHeight(); y++)
		{
			for(int x=0; x<output.getWidth(); x++)
			{
				for(int c=0; c<output.getChannels(); c++)
				{
					float max = Float.NEGATIVE_INFINITY;

					int inputYStart = y * stride;
					int inputXStart = x * stride;

					for(int dy = 0; dy < poolSize; dy++)
					{
						for(int dx = 0; dx < poolSize; dx++)
						{
							int inputY = inputYStart + dy;
							int inputX = inputXStart + dx;
							
							float value = input.get(inputY, inputX, c);
							
							if(value > max)
								max = value;
						}
					}

					output.set(y, x, c, max);
				}
			}
		}
		
		return output;
	}
	
	public int getPoolSize()
	{
		return poolSize;
	}
	
	public int getStride()
	{
		return stride;
	}
}
