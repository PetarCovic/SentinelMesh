package com.sentinelmesh.edge.cnn;

public class Tensor1D 
{
	private final int length;
	private final float[] data;
	
	public Tensor1D(int length)
	{
		if(length<=0)
			throw new IllegalArgumentException("Length must be greater than 0");
		
		this.length=length;
		this.data=new float[length];
	}
	
	public Tensor1D(float[] data)
	{
		if(data==null || data.length==0)
			throw new IllegalArgumentException("Data cannot be null or empty");
		
		this.length=data.length;		
		this.data=data;
	}
	
	public int getLength()
	{
		return length;
	}
	
	public float get(int index)
	{
		validateIndex(index);
		
		return data[index];
	}
	
	public void set(int index, float value)
	{
		validateIndex(index);
		
		data[index]=value;
	}
	
	public float[] getData()
	{
		return data;
	}
	
	private void validateIndex(int index)
	{
		if(index < 0 || index >= length)
			throw new IllegalArgumentException("Index must be between 0 and " + (length - 1));
	}
}
