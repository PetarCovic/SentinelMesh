package com.sentinelmesh.edge.cnn;

public class Tensor3D 
{
	private final int height;
	private final int width;
	private final int channels;
	private final float[][][] data;
	
	public Tensor3D(int height, int width, int channels)
	{
		if(height<=0)
			throw new IllegalArgumentException("Height cannot be less than or equal to 0");
		
		if(width<=0)
			throw new IllegalArgumentException("Width cannot be less than or equal to 0");
		
		if(channels<=0)
			throw new IllegalArgumentException("Channels cannot be less than or equal to 0");
		
		this.height=height;
		this.width=width;
		this.channels=channels;
		this.data=new float[height][width][channels];
	}
	
	public Tensor3D(float[][][] data)
	{
		if(data==null || data.length==0)
			throw new IllegalArgumentException("Data cannot be null or empty");
		
		if(data[0]==null || data[0].length==0)
			throw new IllegalArgumentException("Data[0] cannot be null or empty");
		
		if(data[0][0]==null || data[0][0].length==0)
			throw new IllegalArgumentException("Data[0][0] cannot be null or empty");
		
		this.height=data.length;
		this.width=data[0].length;
		this.channels=data[0][0].length;
		
		for(int y = 0; y < data.length; y++)
		{
			if(data[y] == null || data[y].length != data[0].length)
				throw new IllegalArgumentException("All rows must be non-null and have the same width");
			
			for(int x = 0; x < data[y].length; x++)
			{
				if(data[y][x] == null || data[y][x].length != data[0][0].length)
					throw new IllegalArgumentException("All pixels must be non-null and have the same channel count");
			}
		}
		this.data=data;
	}
	
	public int getHeight()
	{
		return height;
	}
	
	public int getWidth()
	{
		return width;
	}
	
	public int getChannels()
	{
		return channels;
	}
	
	public float get(int y, int x, int channel)
	{
		validateIndex(y, x, channel);
		
		return data[y][x][channel];
	}
	
	public void set(int y, int x, int channel, float value)
	{
		validateIndex(y, x, channel);
		
		data[y][x][channel]=value;
	}
	
	public float[][][] getData()
	{
		return data;
	}
	
	private void validateIndex(int y, int x, int channel)
	{
		if(y<0 || y>=height)
			throw new IllegalArgumentException("Y index must be between 0 and "+(height-1));
		
		if(x<0 || x>=width)
			throw new IllegalArgumentException("X index must be between 0 and "+(width-1));
		
		if(channel<0 || channel>=channels)
			throw new IllegalArgumentException("Channel index must be between 0 and "+(channels-1));
	}
}
