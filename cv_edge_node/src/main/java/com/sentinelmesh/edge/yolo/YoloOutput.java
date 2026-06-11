package com.sentinelmesh.edge.yolo;

import java.util.ArrayList;
import java.util.List;

public class YoloOutput 
{
	private final int gridSize;
	private final List<YoloGridCell> cells;
	
	public YoloOutput(int gridSize, List<YoloGridCell> cells)
	{
		if(gridSize<=0)
			throw new IllegalArgumentException("GridSize cannot be 0 or less");
		
		if(cells==null)
			throw new IllegalArgumentException("Cells cannot be null");
		
		if(cells.size()!=gridSize*gridSize)
			throw new IllegalArgumentException("Cells size must equal gridSize*gridSize");
		
		for(YoloGridCell cell : cells)
		{
			if(cell==null)
				throw new IllegalArgumentException("Cells cannot contain null");
			
			if(cell.getGridSize()!=gridSize)
				throw new IllegalArgumentException("Cell grid size must match output grid size");
		}
		
		this.gridSize=gridSize;
		this.cells=List.copyOf(cells);
	}
	
	public int getGridSize()
	{
		return gridSize;
	}
	
	public List<YoloGridCell> getCells()
	{
		return cells;
	}
	
	public boolean hasPredictions()
	{
		for(YoloGridCell cell : cells)
		{
			if(cell.hasPredictions())
				return true;
		}
		
		return false;
	}
	
	public List<YoloPrediction> getAllPredictions()
	{
		List<YoloPrediction> predictions=new ArrayList<>();
		
		for(YoloGridCell cell : cells)
		{
			if(cell.hasPredictions())
			{
				predictions.addAll(cell.getPredictions());
			}
		}
		
		return List.copyOf(predictions);
	}
	
	public YoloGridCell getCell(int row, int col)
	{
		if(row<0)
			throw new IllegalArgumentException("Row cannot be negative");
		
		if(row>=gridSize)
			throw new IllegalArgumentException("Row must be smaller than gridSize");
		
		if(col<0)
			throw new IllegalArgumentException("Col cannot be negative");
		
		if(col>=gridSize)
			throw new IllegalArgumentException("Col must be smaller than gridSize");
		
		return cells.get(row*gridSize+col);
	}
}
