package com.sentinelmesh.edge.yolo.v1;

import java.util.ArrayList;
import java.util.List;

import com.sentinelmesh.edge.yolo.YoloBoundingBox;
import com.sentinelmesh.edge.yolo.YoloInput;
import com.sentinelmesh.edge.yolo.YoloPrediction;

public class YoloV1OutputDecoder 
{
	private final String className;
	
	public YoloV1OutputDecoder(String className)
	{
		if(className == null || className.isBlank())
			throw new IllegalArgumentException("Class name cannot be null or blank");
		
		this.className = className;
	}
	
	public YoloV1Output decode(YoloV1RawOutput rawOutput, YoloInput input)
	{
		if(rawOutput == null)
			throw new IllegalArgumentException("YoloRawOutput cannot be null");
		
		if(input == null)
			throw new IllegalArgumentException("YoloInput cannot be null");
		
		if(rawOutput.getClassCount() != 1)
			throw new IllegalArgumentException("This decoder currently supports person-only YOLO output");
		
		List<YoloV1GridCell> cells = new ArrayList<>();
		int gridSize = rawOutput.getGridSize();
		
		for(int row = 0; row < gridSize; row++)
		{
			for(int col = 0; col < gridSize; col++)
			{
				YoloV1GridCell cell = decodeCell(rawOutput, input, row, col);
				cells.add(cell);
			}
		}
		
		return new YoloV1Output(gridSize, cells);
	}
	
	private YoloV1GridCell decodeCell(
			YoloV1RawOutput rawOutput,
			YoloInput input,
			int row,
			int col)
	{
		double personProbability = clampProbability(rawOutput.getValue(row, col, 0));
		List<YoloPrediction> predictions = new ArrayList<>();
		
		for(int boxIndex = 0; boxIndex < rawOutput.getBoxesPerCell(); boxIndex++)
		{
			YoloPrediction prediction = decodeBox(
					rawOutput,
					input,
					row,
					col,
					boxIndex,
					personProbability
			);
			
			if(prediction != null)
				predictions.add(prediction);
		}
		
		return new YoloV1GridCell(
				row,
				col,
				rawOutput.getGridSize(),
				personProbability,
				predictions
		);
	}
	
	private YoloPrediction decodeBox(
			YoloV1RawOutput rawOutput,
			YoloInput input,
			int row,
			int col,
			int boxIndex,
			double personProbability)
	{
		if(boxIndex < 0 || boxIndex >= rawOutput.getBoxesPerCell())
			throw new IllegalArgumentException("Box index is out of range");
		
		int boxStartOffset = rawOutput.getClassCount() + boxIndex * 5;
		
		double rawX = clampProbability(rawOutput.getValue(row, col, boxStartOffset));
		double rawY = clampProbability(rawOutput.getValue(row, col, boxStartOffset + 1));
		double rawWidth = rawOutput.getValue(row, col, boxStartOffset + 2);
		double rawHeight = rawOutput.getValue(row, col, boxStartOffset + 3);
		double objectness = clampProbability(rawOutput.getValue(row, col, boxStartOffset + 4));
		
		if(rawWidth <= 0 || rawHeight <= 0)
			return null;
		
		int gridSize = rawOutput.getGridSize();
		
		double cellWidth = input.getInputWidth() / (double) gridSize;
		double cellHeight = input.getInputHeight() / (double) gridSize;
		
		double centerX = (col + rawX) * cellWidth;
		double centerY = (row + rawY) * cellHeight;
		
		double boxWidth = rawWidth * input.getInputWidth();
		double boxHeight = rawHeight * input.getInputHeight();
		
		YoloBoundingBox clampedBox = createClampedBoxFromCenter(
				centerX,
				centerY,
				boxWidth,
				boxHeight,
				input
		);
		
		if(clampedBox == null)
			return null;
		
		return new YoloPrediction(
				clampedBox,
				objectness,
				personProbability,
				className
		);
	}
	
	private double clampProbability(double value)
	{
		if(value < 0.0)
			return 0.0;
		
		if(value > 1.0)
			return 1.0;
		
		return value;
	}
	
	private YoloBoundingBox createClampedBoxFromCenter(
			double centerX,
			double centerY,
			double boxWidth,
			double boxHeight,
			YoloInput input)
	{
		if(input == null)
			throw new IllegalArgumentException("YoloInput cannot be null");
		
		if(boxWidth <= 0)
			throw new IllegalArgumentException("Box width must be greater than 0");
		
		if(boxHeight <= 0)
			throw new IllegalArgumentException("Box height must be greater than 0");
		
		double inputX1 = centerX - boxWidth / 2.0;
		double inputY1 = centerY - boxHeight / 2.0;
		double inputX2 = centerX + boxWidth / 2.0;
		double inputY2 = centerY + boxHeight / 2.0;
		
		double scaleX = input.getOriginalWidth() / (double) input.getInputWidth();
		double scaleY = input.getOriginalHeight() / (double) input.getInputHeight();
		
		double originalX1 = inputX1 * scaleX;
		double originalY1 = inputY1 * scaleY;
		double originalX2 = inputX2 * scaleX;
		double originalY2 = inputY2 * scaleY;
		
		double clampedX1 = Math.max(0.0, originalX1);
		double clampedY1 = Math.max(0.0, originalY1);
		double clampedX2 = Math.min(input.getOriginalWidth(), originalX2);
		double clampedY2 = Math.min(input.getOriginalHeight(), originalY2);
		
		double clampedWidth = clampedX2 - clampedX1;
		double clampedHeight = clampedY2 - clampedY1;
		
		if(clampedWidth <= 0 || clampedHeight <= 0)
			return null;
		
		return new YoloBoundingBox(
				clampedX1,
				clampedY1,
				clampedWidth,
				clampedHeight
		);
	}
}