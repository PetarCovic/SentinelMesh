package com.sentinelmesh.edge.yolo.v1;

import java.util.List;

import com.sentinelmesh.edge.cnn.Activation1DLayer;
import com.sentinelmesh.edge.cnn.FlattenLayer;
import com.sentinelmesh.edge.cnn.FullyConnectedLayer;
import com.sentinelmesh.edge.cnn.Layer;
import com.sentinelmesh.edge.cnn.Tensor1D;
import com.sentinelmesh.edge.cnn.Tensor3D;
import com.sentinelmesh.edge.yolo.YoloInput;

public class YoloV1Model 
{
	private final int gridSize;
	private final int boxesPerCell;
	private final int classCount;
	
	private final List<Layer<Tensor3D, Tensor3D>> featureLayers;
	private final FlattenLayer flattenLayer;
	private final FullyConnectedLayer hiddenFullyConnectedLayer;
	private final Activation1DLayer hiddenActivationLayer;
	private final FullyConnectedLayer outputFullyConnectedLayer;
	
	public YoloV1Model(
			int gridSize,
			int boxesPerCell,
			int classCount,
			List<Layer<Tensor3D, Tensor3D>> featureLayers,
			FlattenLayer flattenLayer,
			FullyConnectedLayer hiddenFullyConnectedLayer,
			Activation1DLayer hiddenActivationLayer,
			FullyConnectedLayer outputFullyConnectedLayer
			)
	{
		if(gridSize<=0)
			throw new IllegalArgumentException("Gridsize must be greater than 0");
		
		if(boxesPerCell<=0)
			throw new IllegalArgumentException("BoxesPerCell must be greater than 0");
		
		if(classCount<=0)
			throw new IllegalArgumentException("ClassCount must be greater than 0");
		
		if(featureLayers==null)
			throw new IllegalArgumentException("FeatureLayers cannot be null");
		
		for(Layer<Tensor3D, Tensor3D> layer : featureLayers)
			if(layer==null)
				throw new IllegalArgumentException("FeatureLayers cannot contain null");
		
		if(flattenLayer==null)
			throw new IllegalArgumentException("FlattenLayer cannot be null");
		
		if(hiddenFullyConnectedLayer==null)
			throw new IllegalArgumentException("HiddenFullyConnectedLayer cannot be null");
		
		if(hiddenActivationLayer==null)
			throw new IllegalArgumentException("HiddenActivationLayer cannot be null");
		
		if(outputFullyConnectedLayer==null)
			throw new IllegalArgumentException("OutputFullyConnectedLayer cannot be null");
		
		this.gridSize=gridSize;
		this.boxesPerCell=boxesPerCell;
		this.classCount=classCount;
		this.featureLayers=List.copyOf(featureLayers);
		this.flattenLayer=flattenLayer;
		this.hiddenFullyConnectedLayer=hiddenFullyConnectedLayer;
		this.hiddenActivationLayer=hiddenActivationLayer;
		this.outputFullyConnectedLayer=outputFullyConnectedLayer;
	}
	
	public YoloV1RawOutput forward(YoloInput input)
	{
		if(input==null)
			throw new IllegalArgumentException("Input cannot be null");
		
		Tensor3D inputTensor=new Tensor3D(input.getImageData());
		
		Tensor3D featuredTensor=runFeatureLayers(inputTensor);
		
		Tensor1D flattenedTensor=flattenLayer.forward(featuredTensor);
		
		Tensor1D hiddenConnectedTensor=hiddenFullyConnectedLayer.forward(flattenedTensor);
		
		Tensor1D hiddenActivatedTensor=hiddenActivationLayer.forward(hiddenConnectedTensor);
		
		Tensor1D output=outputFullyConnectedLayer.forward(hiddenActivatedTensor);
		
		int expectedLength=getExpectedOutputLength();
		
		if(output.getLength()!=expectedLength)
			throw new IllegalStateException("Output length "+output.getLength()
			+" does not equal "+expectedLength);
		
		return new YoloV1RawOutput(gridSize, boxesPerCell, classCount, output.getData());
	}
	
	private Tensor3D runFeatureLayers(Tensor3D inputTensor)
	{
		if(inputTensor==null)
			throw new IllegalArgumentException("InputTensor cannot be null");
		
		Tensor3D current=inputTensor;
		
		for(Layer<Tensor3D, Tensor3D> layer : featureLayers)
		{
			current=layer.forward(current);
		}
		
		return current;
	}
	
	public int getExpectedOutputLength()
	{
		return gridSize * gridSize * (classCount + boxesPerCell * 5);
	}
}
