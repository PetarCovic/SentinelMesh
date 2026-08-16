package com.sentinelmesh.edge.recording;

import java.util.Optional;

import com.sentinelmesh.edge.camera.Frame;

public class ContinuousRecordingCoordinator implements AutoCloseable
{
	private final StreamingRecordingEncoder streamingEncoder;
	private final RecordingSpoolService spoolService;
	private final RecordingSpoolCapacityManager capacityManager;
	
	private boolean closed;
	
	public ContinuousRecordingCoordinator(
			StreamingRecordingEncoder streamingEncoder,
			RecordingSpoolService spoolService,
			RecordingSpoolCapacityManager capacityManager
			)
	{
		if(streamingEncoder==null)
			throw new IllegalArgumentException("StreamingEncoder cannot be null");
		
		if(spoolService==null)
			throw new IllegalArgumentException("SpoolService cannot be null");
		
		if(capacityManager==null)
			throw new IllegalArgumentException("CapacityManager cannot be null");
		
		this.streamingEncoder=streamingEncoder;
		this.spoolService=spoolService;
		this.capacityManager=capacityManager;
		this.closed=false;
	}
	
	public synchronized void acceptFrame(Frame frame)
	{
		if(frame==null || frame.isEmpty())
			throw new IllegalArgumentException("Frame cannot be null or empty");
		
		if(closed)
			throw new IllegalStateException("ContinuousRecordingCoordinator is closed");
		
		Optional<RecordingSpoolEntry> completedEntry=streamingEncoder.acceptFrame(frame);
		
		if(completedEntry.isPresent())
		{
			spoolService.moveToPending(completedEntry.get());
			capacityManager.enforceCapacity();
		}
	}
	
	public synchronized void finalizeCurrentSegment()
	{
		if(closed)
			throw new IllegalStateException("ContinuousRecordingCoordinator is closed");
		
		Optional<RecordingSpoolEntry> finalizedEntry=streamingEncoder.finalizeCurrentSegment();
		
		if(finalizedEntry.isPresent())
		{
			spoolService.moveToPending(finalizedEntry.get());
			capacityManager.enforceCapacity();
		}
	}
	
	@Override
	public synchronized void close()
	{
		if(closed)
		    return;

		try
		{
		    finalizeCurrentSegment();
		}
		finally
		{
		    try
		    {
		        streamingEncoder.close();
		    }
		    finally
		    {
		        closed = true;
		    }
		}
	}
}
