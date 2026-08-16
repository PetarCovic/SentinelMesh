package com.sentinelmesh.edge.media;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import com.sentinelmesh.edge.camera.Frame;

public class FrameRingBuffer 
{
	private final int maxFrames;
	private ArrayDeque<Frame> frameDeque;
	
	public FrameRingBuffer(int maxFrames)
	{
		if(maxFrames<=0)
			throw new IllegalArgumentException("maxFrames must be greater than 0");
		
		this.maxFrames=maxFrames;
		this.frameDeque=new ArrayDeque<>();
	}
	
	public synchronized void add(Frame frame)
	{
		if(frame==null || frame.isEmpty())
			throw new IllegalArgumentException("Frame cannot be empty");
		
		if(frameDeque.size()<maxFrames)
			frameDeque.add(frame.copy());
		else
		{
			Frame oldFrame=frameDeque.pop();
			oldFrame.close();
			frameDeque.add(frame.copy());
		}
	}
	
	public synchronized List<Frame> snapshot()
	{
		ArrayList<Frame> frames=new ArrayList<>();
		
		for(Frame frame : frameDeque)
		{
			frames.add(frame.copy());
		}
		
		return frames;
	}
	
	public synchronized int size()
	{
		return frameDeque.size();
	}
	
	public synchronized void clear()
	{
		while(frameDeque.size()!=0)
			frameDeque.pop().close();
	}
	
	public synchronized void close()
	{
		clear();
	}
}
