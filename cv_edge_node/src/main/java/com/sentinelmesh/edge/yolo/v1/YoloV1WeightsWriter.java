package com.sentinelmesh.edge.yolo.v1;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class YoloV1WeightsWriter 
{
	private static final String HEADER="SMYOLOV1";
	private static final int VERSION=2;
	
	public void write(YoloV1Weights weights, Path path)
	{
		if(weights==null)
			throw new IllegalArgumentException("Weights cannot be null");
		
		if(path==null)
			throw new IllegalArgumentException("Path cannot be null");
		
		try 
		{
			Path parent=path.getParent();
			
			if(parent!=null)
				Files.createDirectories(parent);
			
			try(DataOutputStream out = new DataOutputStream(
					new BufferedOutputStream(new FileOutputStream(path.toFile())))) 
			{
				out.writeUTF(HEADER);
				out.writeInt(VERSION);
				
				writeConvLayers(out, weights.getConvLayers());
				
				write2D(out, weights.getHiddenWeights());
				write1D(out, weights.getHiddenBiases());
				
				write2D(out, weights.getOutputWeights());
				write1D(out, weights.getOutputBiases());
			}
		}
		catch(IOException ex)
		{
			throw new IllegalStateException("Failed to write YOLOv1 weights to: " + path, ex);
		}
	}
	
	private void writeConvLayers(DataOutputStream out, List<YoloV1ConvLayerWeights> convLayers) throws IOException
	{
		out.writeInt(convLayers.size());
		
		for(YoloV1ConvLayerWeights layer : convLayers)
		{
			out.writeInt(layer.getStride());
			out.writeUTF(layer.getPadding().name());
			out.writeBoolean(layer.isApplyLeakyReluAfter());
			out.writeBoolean(layer.isApplyMaxPoolAfter());
			out.writeInt(layer.getMaxPoolSize());
			out.writeInt(layer.getMaxPoolStride());
			
			write4D(out, layer.getFilters());
			write1D(out, layer.getBiases());
		}
	}
	
	private void write4D(DataOutputStream out, float[][][][] values) throws IOException
	{
		validate4D(values);
		
		out.writeInt(values.length);
		out.writeInt(values[0].length);
		out.writeInt(values[0][0].length);
		out.writeInt(values[0][0][0].length);
		
		for(int i=0; i<values.length; i++)
		{
			for(int j=0; j<values[i].length; j++)
			{
				for(int k=0; k<values[i][j].length; k++)
				{
					for(int l=0; l<values[i][j][k].length; l++)
					{
						out.writeFloat(values[i][j][k][l]);
					}
				}
			}
		}
	}
	
	private void write2D(DataOutputStream out, float[][] values) throws IOException
	{
		validate2D(values);
		
		out.writeInt(values.length);
		out.writeInt(values[0].length);
		
		for(int i=0; i<values.length; i++)
		{
			for(int j=0; j<values[i].length; j++)
			{
				out.writeFloat(values[i][j]);
			}
		}
	}
	
	private void write1D(DataOutputStream out, float[] values) throws IOException
	{
		if(values==null || values.length==0)
			throw new IllegalArgumentException("1D values cannot be null or empty");
		
		out.writeInt(values.length);
		
		for(int i=0; i<values.length; i++)
		{
			out.writeFloat(values[i]);
		}
	}
	
	private void validate4D(float[][][][] values)
	{
		if(values == null || values.length == 0)
			throw new IllegalArgumentException("4D values cannot be null or empty");
		
		int expectedB = -1;
		int expectedC = -1;
		int expectedD = -1;
		
		for(int a = 0; a < values.length; a++)
		{
			if(values[a] == null || values[a].length == 0)
				throw new IllegalArgumentException("4D values[" + a + "] cannot be null or empty");
			
			if(expectedB == -1)
				expectedB = values[a].length;
			else if(values[a].length != expectedB)
				throw new IllegalArgumentException("4D values must be rectangular in dimension 2");
			
			for(int b = 0; b < values[a].length; b++)
			{
				if(values[a][b] == null || values[a][b].length == 0)
					throw new IllegalArgumentException("4D values[" + a + "][" + b + "] cannot be null or empty");
				
				if(expectedC == -1)
					expectedC = values[a][b].length;
				else if(values[a][b].length != expectedC)
					throw new IllegalArgumentException("4D values must be rectangular in dimension 3");
				
				for(int c = 0; c < values[a][b].length; c++)
				{
					if(values[a][b][c] == null || values[a][b][c].length == 0)
					{
						throw new IllegalArgumentException(
								"4D values[" + a + "][" + b + "][" + c + "] cannot be null or empty"
						);
					}
					
					if(expectedD == -1)
						expectedD = values[a][b][c].length;
					else if(values[a][b][c].length != expectedD)
						throw new IllegalArgumentException("4D values must be rectangular in dimension 4");
				}
			}
		}
	}
	
	private void validate2D(float[][] values)
	{
		if(values == null || values.length == 0)
			throw new IllegalArgumentException("2D values cannot be null or empty");
		
		int expectedWidth = -1;
		
		for(int i = 0; i < values.length; i++)
		{
			if(values[i] == null || values[i].length == 0)
				throw new IllegalArgumentException("2D values[" + i + "] cannot be null or empty");
			
			if(expectedWidth == -1)
				expectedWidth = values[i].length;
			else if(values[i].length != expectedWidth)
				throw new IllegalArgumentException("2D values must be rectangular");
		}
	}
}