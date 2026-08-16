package com.sentinelmesh.edge.media;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.sentinelmesh.edge.camera.Frame;
import com.sentinelmesh.edge.client.RecordingSegmentUploadClient;

@Deprecated
public class ContinuousRecordingManager 
{
	private final RecordingSegmentBuffer recordingSegmentBuffer;
	private final VideoClipEncoder videoClipEncoder;
	private final RecordingSegmentUploadClient recordingSegmentUploadClient;
	private final UUID deviceId;
	private final String apiKey;
	private final boolean enabled;
	private final int recordingFPS;
	
	private final ExecutorService recordingExecutor;
	private volatile boolean closed;
	
	public ContinuousRecordingManager(
			RecordingSegmentBuffer recordingSegmentBuffer,
			VideoClipEncoder videoClipEncoder,
			RecordingSegmentUploadClient recordingSegmentUploadClient,
			UUID deviceId,
			String apiKey,
			boolean enabled,
			int recordingFPS,
			int queueCapacity
			)
	{
		if(recordingSegmentBuffer==null)
			throw new IllegalArgumentException("RecordingSegmentBuffer cannot be null");
		
		if(videoClipEncoder==null)
			throw new IllegalArgumentException("VideoClipEncoder cannot be null");
		
		if(recordingSegmentUploadClient==null)
			throw new IllegalArgumentException("RecordingSegmentUploadClient cannot be null");
			
		if(deviceId==null)
			throw new IllegalArgumentException("DeviceId cannot be null");
		
		if(apiKey==null || apiKey.isBlank())
			throw new IllegalArgumentException("ApiKey cannot be null or blank");
		
		if(recordingFPS<=0)
			throw new IllegalArgumentException("FPS must be greater than 0");
		
		if(queueCapacity<=0)
			throw new IllegalArgumentException("QueueCapacity must be greater than 0");
			
		this.recordingSegmentBuffer=recordingSegmentBuffer;
		this.videoClipEncoder=videoClipEncoder;
		this.recordingSegmentUploadClient=recordingSegmentUploadClient;
		this.deviceId=deviceId;
		this.apiKey=apiKey;
		this.enabled=enabled;
		this.recordingFPS=recordingFPS;
		
		this.recordingExecutor=new ThreadPoolExecutor(
				1, 
				1, 
				0L,
				TimeUnit.MILLISECONDS,
				new ArrayBlockingQueue<Runnable>(queueCapacity), 
				new ThreadPoolExecutor.AbortPolicy());
		this.closed=false;
	}
	
	public synchronized void acceptFrame(Frame frame)
	{		
		try
		{
			if(frame==null)
				throw new IllegalArgumentException("Frame cannot be null");
			
			if(!enabled || closed)
			{				
				frame.close();
				return;
			}
			
			if(frame.isEmpty())
			{
				frame.close();
				throw new IllegalArgumentException("Frame cannot be empty");
			}
			
			recordingSegmentBuffer.add(frame);
			
			if(!recordingSegmentBuffer.isSegmentReady())
				return;
			
			CompletedRecordingSegment completedRecordingSegment=
					recordingSegmentBuffer.drainSegment();
			
			submitSegment(completedRecordingSegment);
			
		}
		catch(Exception ex)
		{
			System.out.println("Continuous recording segment failed: " + ex.getMessage());
		}
	}
	
	private void submitSegment(CompletedRecordingSegment completedRecordingSegment)
	{
	    RecordingSegmentTask task =
	            new RecordingSegmentTask(
	                    completedRecordingSegment,
	                    videoClipEncoder,
	                    recordingSegmentUploadClient,
	                    deviceId,
	                    apiKey
	            );

	    try
	    {
	        recordingExecutor.execute(task);
	    }
	    catch(RejectedExecutionException ex)
	    {
	        task.close();

	        System.out.println(
	                "Recording segment rejected because the upload queue is full"
	        );
	    }
	    catch(RuntimeException ex)
	    {
	        task.close();
	        throw ex;
	    }

	}
	
	public synchronized void close()
	{
		if(closed)
			return;
		
		closed=true;
		
		if(recordingSegmentBuffer.size()>=recordingFPS)
		{
			submitSegment(recordingSegmentBuffer.drainSegment());
		}

		recordingSegmentBuffer.close();
		
		recordingExecutor.shutdown();
		try {
			boolean terminated=recordingExecutor.awaitTermination(90, TimeUnit.SECONDS);
			
			if(!terminated)
			{
				List<Runnable> abandonedTasks=recordingExecutor.shutdownNow();
				
				closeAbandonedTasks(abandonedTasks);
			}
		} catch (InterruptedException e) 
		{
			List<Runnable> abandonedTasks=recordingExecutor.shutdownNow();

		    closeAbandonedTasks(abandonedTasks);

		    Thread.currentThread().interrupt();
		}
	}
	
	private void closeAbandonedTasks(List<Runnable> abandonedTasks)
	{
	    for(Runnable task : abandonedTasks)
	    {
	        if(task instanceof RecordingSegmentTask recordingTask)
	            recordingTask.close();
	    }
	}
}
