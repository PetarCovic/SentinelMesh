package com.sentinelmesh.edge.recording;

import java.util.List;

import com.sentinelmesh.edge.audio.AudioSampleBuffer;
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
	
	public synchronized void acceptFrame(
			Frame frame,
			List<AudioSampleBuffer> audioChunks,
			boolean audioRunning
			)
	{
		if(frame==null || frame.isEmpty())
			throw new IllegalArgumentException("Frame cannot be null or empty");
		
		if(closed)
			throw new IllegalStateException("ContinuousRecordingCoordinator is closed");
		
		publishCompleted(streamingEncoder.acceptFrame(
				frame,
				audioChunks,
				audioRunning,
				capacityManager::enforceCapacity));
	}
	
	public synchronized void finish(List<AudioSampleBuffer> audioChunks)
	{
		if(closed)
			return;
		
		publishCompleted(streamingEncoder.finishRemainingSegments(audioChunks));
	}
	
	@Override
	public synchronized void close()
	{
		if(closed)
		    return;

		try
		{
		    finish(List.of());
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
	
	private synchronized void publishCompleted(
			List<RecordingSpoolEntry> entries
			)
	{
		for(RecordingSpoolEntry entry : entries)
		{
			spoolService.moveToPending(entry);
		}
	}
}
