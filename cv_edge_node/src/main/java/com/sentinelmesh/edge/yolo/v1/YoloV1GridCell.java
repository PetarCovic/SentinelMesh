package com.sentinelmesh.edge.yolo.v1;

import java.util.List;

import com.sentinelmesh.edge.yolo.YoloPrediction;

public class YoloV1GridCell 
{
	private final int row;
	private final int col;
	private final int gridSize;
	private final double personProbability;
	private final List<YoloPrediction> predictions;
	
	public YoloV1GridCell(
			int row,
			int col,
			int gridSize,
			double personProbability,
			List<YoloPrediction> predictions
			)
	{
		if(row<0)
			throw new IllegalArgumentException("Row cannot be negative");
		
		if(col<0)
			throw new IllegalArgumentException("Col cannot be negative");
		
		if(gridSize<=0)
			throw new IllegalArgumentException("gridSize must be greater than 0");
		
		if(row>=gridSize)
			throw new IllegalArgumentException("Row must be smaller than gridSize");
		
		if(col>=gridSize)
			throw new IllegalArgumentException("Col must be smaller than gridSize");
		
		if(personProbability<0 || personProbability>1)
			throw new IllegalArgumentException("PersonProbability must be between 0.0 and 1.0");
		
		if(predictions==null)
			throw new IllegalArgumentException("Predictions cannot be null");
		
		for(YoloPrediction prediction : predictions)
		{
			if(prediction==null)
				throw new IllegalArgumentException("Predictions cannot contain null");
		}
		
		this.row=row;
		this.col=col;
		this.gridSize=gridSize;
		this.personProbability=personProbability;
		this.predictions=List.copyOf(predictions);
	}
	
	public int getRow()
	{
		return row;
	}
	
	public int getCol()
	{
		return col;
	}
	
	public int getGridSize()
	{
		return gridSize;
	}
	
	public double getPersonProbability()
	{
		return personProbability;
	}
	
	public List<YoloPrediction> getPredictions()
	{
		return predictions;
	}
	
	public boolean hasPredictions()
	{
		return !predictions.isEmpty();
	}
	
	public int getCellIndex()
	{
		return row*gridSize+col;
	}
}
