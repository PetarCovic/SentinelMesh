package com.sentinelmesh.edge.yolo.v1;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.sentinelmesh.edge.cnn.Activation1DLayer;
import com.sentinelmesh.edge.cnn.ConvolutionLayer;
import com.sentinelmesh.edge.cnn.FlattenLayer;
import com.sentinelmesh.edge.cnn.FullyConnectedLayer;
import com.sentinelmesh.edge.cnn.Layer;
import com.sentinelmesh.edge.cnn.LeakyReluLayer;
import com.sentinelmesh.edge.cnn.MaxPoolLayer;
import com.sentinelmesh.edge.cnn.Padding;
import com.sentinelmesh.edge.cnn.Tensor3D;
import com.sentinelmesh.edge.cnn.WeightInitializer;

public class YoloV1ModelFactory 
{
	private YoloV1ModelFactory()
	{
		
	}
	
	public static YoloV1Model createTinyTestModel()
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
		float[] hiddenBiases = WeightInitializer.createBiases(hiddenSize, 0.0f);
		
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
		
		return new YoloV1Model(
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
	
	public static YoloV1Model createRandomPersonModel()
	{
		int gridSize = 7;
		int boxesPerCell = 2;
		int classCount = 1;
		
		List<Layer<Tensor3D, Tensor3D>> featureLayers = new ArrayList<>();
		
		float[][][][] conv1Filters = WeightInitializer.createConvFilters(
				16,
				7,
				7,
				3,
				-0.01f,
				0.01f
		);
		
		float[] conv1Biases = WeightInitializer.createBiases(16, 0.0f);
		
		featureLayers.add(new ConvolutionLayer(conv1Filters, conv1Biases, 2, Padding.SAME));
		featureLayers.add(new LeakyReluLayer());
		featureLayers.add(new MaxPoolLayer(2, 2));
		
		float[][][][] conv2Filters = WeightInitializer.createConvFilters(
				32,
				3,
				3,
				16,
				-0.01f,
				0.01f
		);
		
		float[] conv2Biases = WeightInitializer.createBiases(32, 0.0f);
		
		featureLayers.add(new ConvolutionLayer(conv2Filters, conv2Biases, 1, Padding.SAME));
		featureLayers.add(new LeakyReluLayer());
		featureLayers.add(new MaxPoolLayer(2, 2));
		
		int flattenedLength = 56 * 56 * 32;
		int hiddenSize = 128;
		int outputSize = gridSize * gridSize * (classCount + boxesPerCell * 5);
		
		float[][] hiddenWeights = WeightInitializer.createFullyConnectedWeights(
				hiddenSize,
				flattenedLength,
				-0.01f,
				0.01f
		);
		
		float[] hiddenBiases = WeightInitializer.createBiases(hiddenSize, 0.0f);
		
		float[][] outputWeights = createZeroWeights(outputSize, hiddenSize);
		float[] outputBiases = createRandomTestOutputBiases(gridSize, boxesPerCell, classCount);
		
		FlattenLayer flattenLayer = new FlattenLayer();
		FullyConnectedLayer hiddenFullyConnectedLayer = new FullyConnectedLayer(hiddenWeights, hiddenBiases);
		Activation1DLayer hiddenActivationLayer = new Activation1DLayer();
		FullyConnectedLayer outputFullyConnectedLayer = new FullyConnectedLayer(outputWeights, outputBiases);
		
		return new YoloV1Model(
				gridSize,
				boxesPerCell,
				classCount,
				featureLayers,
				flattenLayer,
				hiddenFullyConnectedLayer,
				hiddenActivationLayer,
				outputFullyConnectedLayer
		);
	}
	
	public static YoloV1Model createFromWeights(Path path)
	{
		YoloV1Weights weights = new YoloV1WeightsLoader().load(path);
		
		return createFromWeights(weights);
	}
	
	public static YoloV1Model createFromWeights(YoloV1Weights weights)
	{
		if(weights == null)
			throw new IllegalArgumentException("Weights cannot be null");
		
		int gridSize = 7;
		int boxesPerCell = 2;
		int classCount = 1;
		
		List<Layer<Tensor3D, Tensor3D>> featureLayers = new ArrayList<>();
		
		for(YoloV1ConvLayerWeights convLayerWeights : weights.getConvLayers())
		{
			featureLayers.add(new ConvolutionLayer(
					convLayerWeights.getFilters(),
					convLayerWeights.getBiases(),
					convLayerWeights.getStride(),
					convLayerWeights.getPadding()
			));
			
			if(convLayerWeights.isApplyLeakyReluAfter())
				featureLayers.add(new LeakyReluLayer());
			
			if(convLayerWeights.isApplyMaxPoolAfter())
			{
				featureLayers.add(new MaxPoolLayer(
						convLayerWeights.getMaxPoolSize(),
						convLayerWeights.getMaxPoolStride()
				));
			}
		}
		
		FlattenLayer flattenLayer = new FlattenLayer();
		
		FullyConnectedLayer hiddenFullyConnectedLayer = new FullyConnectedLayer(
				weights.getHiddenWeights(),
				weights.getHiddenBiases()
		);
		
		Activation1DLayer hiddenActivationLayer = new Activation1DLayer();
		
		FullyConnectedLayer outputFullyConnectedLayer = new FullyConnectedLayer(
				weights.getOutputWeights(),
				weights.getOutputBiases()
		);
		
		return new YoloV1Model(
				gridSize,
				boxesPerCell,
				classCount,
				featureLayers,
				flattenLayer,
				hiddenFullyConnectedLayer,
				hiddenActivationLayer,
				outputFullyConnectedLayer
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
	
	private static float[] createRandomTestOutputBiases(
			int gridSize,
			int boxesPerCell,
			int classCount)
	{
		int valuesPerCell = classCount + boxesPerCell * 5;
		int outputSize = gridSize * gridSize * valuesPerCell;
		
		float[] biases = new float[outputSize];
		
		for(int cellIndex = 0; cellIndex < gridSize * gridSize; cellIndex++)
		{
			int cellStart = cellIndex * valuesPerCell;
			
			// Person probability
			biases[cellStart] = 0.5f;
			
			for(int boxIndex = 0; boxIndex < boxesPerCell; boxIndex++)
			{
				int boxStart = cellStart + classCount + boxIndex * 5;
				
				biases[boxStart] = 0.5f;       // x inside cell
				biases[boxStart + 1] = 0.5f;   // y inside cell
				biases[boxStart + 2] = 0.2f;   // width relative to input
				biases[boxStart + 3] = 0.2f;   // height relative to input
				biases[boxStart + 4] = 0.5f;   // objectness
			}
		}
		
		return biases;
	}
}
