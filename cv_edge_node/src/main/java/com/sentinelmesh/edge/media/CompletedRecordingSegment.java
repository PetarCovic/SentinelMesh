package com.sentinelmesh.edge.media;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sentinelmesh.edge.camera.Frame;

@Deprecated
public class CompletedRecordingSegment implements AutoCloseable
{
	private final List<Frame> frames;
	private final UUID segmentId;
	private final Instant segmentStartTime;
	private final Instant segmentEndTime;
	private final double effectiveFPS;
	private boolean closed;
	
	public CompletedRecordingSegment(
			List<Frame> frames, 
			Instant segmentStartTime, 
			Instant segmentEndTime
			)
	{
		this(frames, UUID.randomUUID(), segmentStartTime, segmentEndTime);
	}
	
	public CompletedRecordingSegment(
			List<Frame> frames, 
			UUID segmentId,
			Instant segmentStartTime, 
			Instant segmentEndTime
			)
	{
		if(frames==null || frames.isEmpty())
			throw new IllegalArgumentException("Frames cannot be null or empty");
		
		for(Frame frame : frames)
			if(frame==null || frame.isEmpty())
				throw new IllegalArgumentException("Frames cannot contain a null or empty frame");
		
		if(segmentStartTime==null)
			throw new IllegalArgumentException("SegmentStartTime cannot be null");

		if(segmentEndTime==null)
			throw new IllegalArgumentException("SegmentEndTime cannot be null");

		if(!segmentEndTime.isAfter(segmentStartTime))
		    throw new IllegalArgumentException("SegmentEndTime must be after segmentStartTime");
		
		
		this.frames=List.copyOf(frames);
		this.segmentId=segmentId;
		this.segmentStartTime=segmentStartTime;
		this.segmentEndTime=segmentEndTime;
		
		double elapsedSeconds=Duration.between(segmentStartTime, segmentEndTime)
				.toNanos()/1_000_000_000.0;
		this.effectiveFPS=((double)frames.size()-1.0)/elapsedSeconds;
				
		this.closed=false;
	}
	
	public List<Frame> getFrames()
	{
		return frames;
	}
	
	public UUID getSegmentId()
	{
		return segmentId;
	}
	
	public Instant getSegmentStartTime()
	{
		return segmentStartTime;
	}
	
	public Instant getSegmentEndTime()
	{
		return segmentEndTime;
	}
	
	public int getFrameCount()
	{
		return frames.size();
	}
	
	public double getEffectiveFPS()
	{
		return effectiveFPS;
	}
	
	public synchronized boolean isClosed()
	{
		return closed;
	}
	
	@Override
	public synchronized void close()
	{
		if(closed)
			return;
		
		for(Frame frame : frames)
			frame.close();
		
		closed=true;
	}
}
