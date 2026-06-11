package com.sentinelmesh.edge.yolo;

import java.util.List;

import com.sentinelmesh.edge.cnn.Activation1DLayer;
import com.sentinelmesh.edge.cnn.FlattenLayer;
import com.sentinelmesh.edge.cnn.FullyConnectedLayer;
import com.sentinelmesh.edge.cnn.Layer;
import com.sentinelmesh.edge.cnn.Tensor3D;

public class YoloModelFactory 
{
	private YoloModelFactory()
	{
		
	}
	
	public static YoloModel createTinyTestModel()
	{
		int gridSize = 1;
		int boxesPerCell = 1;
		int classCount = 1;
		
		int inputHeight = 448;
		int inputWidth = 448;
		int inputChannels = 3;
		
		int flattenedLength = inputHeight * inputWidth * inputChannels;
		int hiddenSize = 8;
		int outputSize = gridSize * gridSize * (classCount + boxesPerCell * 5);
		
		float[][] hiddenWeights = createHiddenWeights(hiddenSize, flattenedLength);
		float[] hiddenBiases = createBiases(hiddenSize, 0.0f);
		
		float[][] outputWeights = createZeroWeights(outputSize, hiddenSize);
		
		/*
		 * Person-only YOLO raw output layout for gridSize=1, boxesPerCell=1, classCount=1:
		 *
		 * offset 0 = person probability
		 * offset 1 = x
		 * offset 2 = y
		 * offset 3 = width
		 * offset 4 = height
		 * offset 5 = objectness
		 *
		 * This dummy output creates one centered box.
		 */
		float[] outputBiases = new float[] {
				0.90f, // person probability
				0.50f, // center x
				0.50f, // center y
				0.35f, // width
				0.45f, // height
				0.85f  // objectness
		};
		
		return new YoloModel(
				gridSize,
				boxesPerCell,
				classCount,
				List.<Layer<Tensor3D, Tensor3D>>of(),
				new FlattenLayer(),
				new FullyConnectedLayer(hiddenWeights, hiddenBiases),
				new Activation1DLayer(),
				new FullyConnectedLayer(outputWeights, outputBiases)
		);
	}
	
	private static float[][] createHiddenWeights(int outputSize, int inputSize)
	{
		if(outputSize <= 0)
			throw new IllegalArgumentException("Output size must be greater than 0");
		
		if(inputSize <= 0)
			throw new IllegalArgumentException("Input size must be greater than 0");
		
		float[][] weights = new float[outputSize][inputSize];
		
		for(int outputIndex = 0; outputIndex < outputSize; outputIndex++)
		{
			for(int inputIndex = 0; inputIndex < inputSize; inputIndex++)
			{
				weights[outputIndex][inputIndex] = 0.000001f;
			}
		}
		
		return weights;
	}
	
	private static float[][] createZeroWeights(int outputSize, int inputSize)
	{
		if(outputSize <= 0)
			throw new IllegalArgumentException("Output size must be greater than 0");
		
		if(inputSize <= 0)
			throw new IllegalArgumentException("Input size must be greater than 0");
		
		return new float[outputSize][inputSize];
	}
	
	private static float[] createBiases(int size, float value)
	{
		if(size <= 0)
			throw new IllegalArgumentException("Size must be greater than 0");
		
		float[] biases = new float[size];
		
		for(int i = 0; i < size; i++)
		{
			biases[i] = value;
		}
		
		return biases;
	}
}
