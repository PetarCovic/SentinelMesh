package com.sentinelmesh.edge.cnn;

public class LeakyReluActivation implements ActivationFunction
{
	private final float alpha;
	
	public LeakyReluActivation()
	{
		this(0.1f);
	}
	
	public LeakyReluActivation(float alpha)
	{
		if(alpha<0.0f)
			throw new IllegalArgumentException("Alpha cannot be negative");
		
		this.alpha=alpha;
	}
	
	@Override
	public float apply(float value) 
	{
		if(value>=0.0f)
			return value;
		
		return alpha*value;
	}
	
	public float getAlpha()
	{
		return alpha;
	}
}
