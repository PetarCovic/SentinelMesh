package com.sentinelmesh.edge.yolo.v1;

public class YoloV1RawOutput 
{
	private final int gridSize;
	private final int boxesPerCell;
	private final int classCount;
	private final float[] values;
	
	public YoloV1RawOutput(
			int gridSize,
			int boxesPerCell,
			int classCount,
			float[] values
			)
	{
		if(gridSize<=0)
			throw new IllegalArgumentException("GridSize cannot be 0 or less");
		
		if(boxesPerCell<1)
			throw new IllegalArgumentException("BoxesPerCell cannot be less than 1");
		
		if(classCount<1)
			throw new IllegalArgumentException("Class Count cannot be less than 1");
		
		if(values==null)
			throw new IllegalArgumentException("Values cannot be null");
		
		int valuesPerCell = classCount + boxesPerCell * 5;
		int expectedLength = gridSize * gridSize * valuesPerCell;
		
		if(values.length!=expectedLength)
			throw new IllegalArgumentException("Value length must match expected length");
			
		this.gridSize=gridSize;
		this.boxesPerCell=boxesPerCell;
		this.classCount=classCount;
		this.values=values.clone();
	}
	
	public int getGridSize()
	{
		return gridSize;
	}
	
	public int getBoxesPerCell()
	{
		return boxesPerCell;
	}
	
	public int getClassCount()
	{
		return classCount;
	}
	
	public int getValuesPerCell()
	{
		return classCount+boxesPerCell*5;
	}
	
	public int getExpectedLength()
	{
		return gridSize*gridSize*getValuesPerCell();
	}
	
	public float getValue(int row, int col, int offset)
	{
		if(row < 0)
			throw new IllegalArgumentException("Row cannot be negative");
		
		if(row >= gridSize)
			throw new IllegalArgumentException("Row must be smaller than gridSize");
		
		if(col < 0)
			throw new IllegalArgumentException("Col cannot be negative");
		
		if(col >= gridSize)
			throw new IllegalArgumentException("Col must be smaller than gridSize");
		
		if(offset < 0)
			throw new IllegalArgumentException("Offset cannot be negative");
		
		if(offset >= getValuesPerCell())
			throw new IllegalArgumentException("Offset must be smaller than valuesPerCell");

		int cellIndex = row * gridSize + col;
		int flatIndex = cellIndex * getValuesPerCell() + offset;

		return values[flatIndex];
	}
	
	public float[] getValues()
	{
		return values.clone();
	}
}
