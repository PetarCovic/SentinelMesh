package com.sentinelmesh.edge.yolo.v1;

import java.util.ArrayList;
import java.util.List;

import com.sentinelmesh.edge.yolo.YoloPrediction;

public class YoloV1Output 
{
	private final int gridSize;
	private final List<YoloV1GridCell> cells;
	
	public YoloV1Output(int gridSize, List<YoloV1GridCell> cells)
	{
		if(gridSize<=0)
			throw new IllegalArgumentException("GridSize cannot be 0 or less");
		
		if(cells==null)
			throw new IllegalArgumentException("Cells cannot be null");
		
		if(cells.size()!=gridSize*gridSize)
			throw new IllegalArgumentException("Cells size must equal gridSize*gridSize");
		
		for(YoloV1GridCell cell : cells)
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
	
	public List<YoloV1GridCell> getCells()
	{
		return cells;
	}
	
	public boolean hasPredictions()
	{
		for(YoloV1GridCell cell : cells)
		{
			if(cell.hasPredictions())
				return true;
		}
		
		return false;
	}
	
	public List<YoloPrediction> getAllPredictions()
	{
		List<YoloPrediction> predictions=new ArrayList<>();
		
		for(YoloV1GridCell cell : cells)
		{
			if(cell.hasPredictions())
			{
				predictions.addAll(cell.getPredictions());
			}
		}
		
		return List.copyOf(predictions);
	}
	
	public YoloV1GridCell getCell(int row, int col)
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
