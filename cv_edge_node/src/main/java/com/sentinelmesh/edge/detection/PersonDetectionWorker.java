package com.sentinelmesh.edge.detection;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sentinelmesh.edge.camera.Frame;

public class PersonDetectionWorker implements AutoCloseable
{
	private final PersonDetector personDetector;
	private final ExecutorService executor;
	private final AtomicBoolean busy;
	private final Duration maxResultAge;

	private volatile List<DetectionResult> latestResults;
	private volatile Instant latestResultTimestamp;
	private volatile long latestResultVersion;

	public PersonDetectionWorker(PersonDetector personDetector)
	{
		this(personDetector, Duration.ofSeconds(1));
	}

	public PersonDetectionWorker(PersonDetector personDetector, Duration maxResultAge)
	{
		if(personDetector==null)
			throw new IllegalArgumentException("Person Detector cannot be null");

		if(maxResultAge==null || maxResultAge.isNegative() || maxResultAge.isZero())
			throw new IllegalArgumentException("MaxResultAge cannot be null and must be positive");

		this.personDetector=personDetector;
		this.executor=Executors.newSingleThreadExecutor();
		this.busy=new AtomicBoolean(false);
		this.maxResultAge=maxResultAge;
		this.latestResults=List.of();
		this.latestResultTimestamp=Instant.EPOCH;
		this.latestResultVersion=0L;
	}

	public boolean submit(Frame frame)
	{
	    if(frame == null || frame.isEmpty())
	        return false;

	    if(!busy.compareAndSet(false, true))
	        return false;

	    Frame frameCopy;

	    try
	    {
            frameCopy=frame.copy();
	    }
	    catch(Exception ex)
	    {
            busy.set(false);
            throw ex;
	    }

	    try
	    {
            executor.submit(() -> {
		        try
		        {
		            List<DetectionResult> results = personDetector.detect(frameCopy);

		            latestResults = List.copyOf(results);
		            latestResultTimestamp = Instant.now();
		            latestResultVersion++;
		        }
		        catch(Exception ex)
		        {
		            System.err.println("Async person detection failed: " + ex.getMessage());
		        }
		        finally
		        {
                    try
                    {
                        frameCopy.close();
                    }
                    finally
                    {
			            busy.set(false);
                    }
		        }
		    });
	    }
	    catch(RejectedExecutionException ex)
	    {
	        try
	        {
	            frameCopy.close();
	        }
	        finally
	        {
	            busy.set(false);
	        }

	        return false;
	    }

	    return true;
	}

	public List<DetectionResult> getLatestResults()
	{
		if(isLatestResultStale())
			return List.of();

		return latestResults;
	}

	public long getLatestResultVersion()
	{
		return latestResultVersion;
	}

	public boolean isBusy()
	{
		return busy.get();
	}

	private boolean isLatestResultStale()
	{
		if(latestResultTimestamp==null)
			return true;

		Duration age=Duration.between(latestResultTimestamp, Instant.now());

		return age.compareTo(maxResultAge)>0;
	}

	@Override
	public void close()
	{
	    boolean interrupted = false;

	    executor.shutdown();

	    try
	    {
	        while(true)
	        {
	            try
	            {
	                if(executor.awaitTermination(30, TimeUnit.SECONDS))
	                    break;
	            }
	            catch(InterruptedException ex)
	            {
	                interrupted = true;
	            }
	        }

	        personDetector.close();
	    }
	    finally
	    {
	        if(interrupted)
	            Thread.currentThread().interrupt();
	    }
	}
}
