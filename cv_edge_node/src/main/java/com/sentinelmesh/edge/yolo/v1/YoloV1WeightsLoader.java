package com.sentinelmesh.edge.yolo.v1;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.sentinelmesh.edge.cnn.Padding;

public class YoloV1WeightsLoader
{
	private static final String HEADER = "SMYOLOV1";
	private static final int VERSION = 2;
	
	public YoloV1Weights load(Path path)
	{
		if(path == null)
			throw new IllegalArgumentException("Path cannot be null");
		
		if(!Files.exists(path))
			throw new IllegalArgumentException("Weights file does not exist: " + path);
		
		if(!Files.isRegularFile(path))
			throw new IllegalArgumentException("Weights path is not a regular file: " + path);
		
		try(DataInputStream in = new DataInputStream(
				new BufferedInputStream(new FileInputStream(path.toFile()))))
		{
			String header = in.readUTF();
			
			if(!HEADER.equals(header))
				throw new IllegalArgumentException("Invalid YOLOv1 weights header: " + header);
			
			int version = in.readInt();
			
			if(version != VERSION)
			{
				throw new IllegalArgumentException(
						"Unsupported YOLOv1 weights version: " + version + ". Expected version: " + VERSION
				);
			}
			
			List<YoloV1ConvLayerWeights> convLayers = readConvLayers(in);
			
			float[][] hiddenWeights = read2D(in);
			float[] hiddenBiases = read1D(in);
			
			float[][] outputWeights = read2D(in);
			float[] outputBiases = read1D(in);
			
			return new YoloV1Weights(
					convLayers,
					hiddenWeights,
					hiddenBiases,
					outputWeights,
					outputBiases
			);
		}
		catch(IOException ex)
		{
			throw new IllegalStateException("Failed to load YOLOv1 weights from: " + path, ex);
		}
	}
	
	private List<YoloV1ConvLayerWeights> readConvLayers(DataInputStream in) throws IOException
	{
		int convLayerCount = readPositiveInt(in, "conv layer count");
		
		List<YoloV1ConvLayerWeights> convLayers = new ArrayList<>();
		
		for(int i = 0; i < convLayerCount; i++)
		{
			int stride = readPositiveInt(in, "conv layer stride");
			Padding padding = readPadding(in);
			boolean applyLeakyReluAfter = in.readBoolean();
			boolean applyMaxPoolAfter = in.readBoolean();
			int maxPoolSize = in.readInt();
			int maxPoolStride = in.readInt();
			
			float[][][][] filters = read4D(in);
			float[] biases = read1D(in);
			
			YoloV1ConvLayerWeights layer = new YoloV1ConvLayerWeights(
					filters,
					biases,
					stride,
					padding,
					applyLeakyReluAfter,
					applyMaxPoolAfter,
					maxPoolSize,
					maxPoolStride
			);
			
			convLayers.add(layer);
		}
		
		return convLayers;
	}
	
	private Padding readPadding(DataInputStream in) throws IOException
	{
		String paddingName = in.readUTF();
		
		try
		{
			return Padding.valueOf(paddingName);
		}
		catch(IllegalArgumentException ex)
		{
			throw new IllegalArgumentException("Invalid padding value in YOLOv1 weights file: " + paddingName, ex);
		}
	}
	
	private float[][][][] read4D(DataInputStream in) throws IOException
	{
		int dimA = readPositiveInt(in, "4D dimA");
		int dimB = readPositiveInt(in, "4D dimB");
		int dimC = readPositiveInt(in, "4D dimC");
		int dimD = readPositiveInt(in, "4D dimD");
		
		float[][][][] values = new float[dimA][dimB][dimC][dimD];
		
		for(int a = 0; a < dimA; a++)
		{
			for(int b = 0; b < dimB; b++)
			{
				for(int c = 0; c < dimC; c++)
				{
					for(int d = 0; d < dimD; d++)
					{
						values[a][b][c][d] = in.readFloat();
					}
				}
			}
		}
		
		return values;
	}
	
	private float[][] read2D(DataInputStream in) throws IOException
	{
		int rows = readPositiveInt(in, "2D rows");
		int cols = readPositiveInt(in, "2D cols");
		
		float[][] values = new float[rows][cols];
		
		for(int i = 0; i < rows; i++)
		{
			for(int j = 0; j < cols; j++)
			{
				values[i][j] = in.readFloat();
			}
		}
		
		return values;
	}
	
	private float[] read1D(DataInputStream in) throws IOException
	{
		int length = readPositiveInt(in, "1D length");
		
		float[] values = new float[length];
		
		for(int i = 0; i < length; i++)
		{
			values[i] = in.readFloat();
		}
		
		return values;
	}
	
	private int readPositiveInt(DataInputStream in, String label) throws IOException
	{
		int value = in.readInt();
		
		if(value <= 0)
			throw new IllegalArgumentException("Invalid " + label + ": " + value);
		
		return value;
	}
}