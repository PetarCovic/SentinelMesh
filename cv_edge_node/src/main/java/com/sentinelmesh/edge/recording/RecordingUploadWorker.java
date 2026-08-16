package com.sentinelmesh.edge.recording;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.sentinelmesh.edge.client.RecordingSegmentUploadClient;
import com.sentinelmesh.edge.exceptions.RecordingSpoolException;

public class RecordingUploadWorker implements Runnable, AutoCloseable
{	
	private final RecordingSpoolDirectories spoolDirectories;
	private final RecordingSpoolMetadataStore metadataStore;
	private final RecordingSpoolService spoolService;
	private final RecordingSegmentUploadClient uploadClient;

	private final String apiKey;
	private final int maximumUploadAttempts;
	private final Duration retryInterval;
	private final boolean retainUploadedRecordings;

	private Thread workerThread;
	private volatile boolean running;
	private boolean closed;
	
	private static final long CLEANUP_BASE_DELAY_SECONDS = 5L;
	private static final long CLEANUP_MAX_DELAY_SECONDS = 300L;
	
	public RecordingUploadWorker(
			RecordingSpoolDirectories spoolDirectories,
			RecordingSpoolMetadataStore metadataStore,
			RecordingSpoolService spoolService,
			RecordingSegmentUploadClient uploadClient,
			String apiKey,
			int maximumUploadAttempts,
			Duration retryInterval,
			boolean retainUploadedRecordings
			)
	{
		if(spoolDirectories==null)
			throw new IllegalArgumentException("SpoolDirectories cannot be null");
		
		if(metadataStore==null)
			throw new IllegalArgumentException("MetadataStore cannot be null");
		
		if(spoolService==null)
			throw new IllegalArgumentException("SpoolService cannot be null");
		
		if(uploadClient==null)
			throw new IllegalArgumentException("UploadClient cannot be null");
		
		if(apiKey==null || apiKey.isBlank())
			throw new IllegalArgumentException("ApiKey cannot be null or blank");
		
		if(maximumUploadAttempts<=0)
			throw new IllegalArgumentException("MaximumUploadAttempts must be greater than 0");
		
		if(retryInterval == null)
			throw new IllegalArgumentException("RetryInterval cannot be null");
		
		if(!retryInterval.isPositive())
			throw new IllegalArgumentException("RetryInterval must be positive");
		
		if(retryInterval.compareTo(Duration.ofSeconds(1)) < 0)
			throw new IllegalArgumentException("RetryInterval must be at least 1 second");
		
		
		this.spoolDirectories=spoolDirectories;
		this.metadataStore=metadataStore;
		this.spoolService=spoolService;
		this.uploadClient=uploadClient;
		this.apiKey=apiKey;
		this.maximumUploadAttempts=maximumUploadAttempts;
		this.retryInterval=retryInterval;
		this.retainUploadedRecordings=retainUploadedRecordings;
	}
	
	public synchronized void start()
	{
		if(closed)
			throw new IllegalStateException("RecordingUploadWorker is closed");
		
		if(running)
			return;
		
		running=true;
		
		workerThread=new Thread(this, "recording-upload-worker");
		
		workerThread.start();
	}
	
	@Override
	public void run()
	{
		while(running)
		{
			try
			{
				Optional<RecordingSpoolEntry> nextEntry=findNextRecording();
				
				if(nextEntry.isEmpty())
				{
					waitForRetryInterval();
					continue;
				}
				else
				{
					processRecording(nextEntry.get());
				}
			}
			catch(Exception ex)
			{
				System.err.println("Recording upload worker failed: "
						+getFailureMessage(ex));
				
				ex.printStackTrace(System.err);
				
				waitForRetryInterval();
			}
		}
	}
	
	@Override
	public synchronized void close()
	{
		if(closed)
			return;
		
		try
		{
			running=false;
			
			if(workerThread==null)
				return;

			workerThread.interrupt();
			awaitWorkerTermination(workerThread);
		}
		finally
		{
			closed=true;
		}
	}
	
	private Optional<RecordingSpoolEntry> findNextRecording()
	{
		Optional<RecordingSpoolEntry> confirmedEntry =
                findOldestEntryWithStatus(
                        spoolDirectories.getUploading(),
                        RecordingSpoolStatus.UPLOAD_CONFIRMED);

        if(confirmedEntry.isPresent())
            return confirmedEntry;

        Optional<RecordingSpoolEntry> uploadingEntry =
    			findOldestEntryWithStatus(
    					spoolDirectories.getUploading(),
    					RecordingSpoolStatus.UPLOADING);
        
    	if(uploadingEntry.isPresent())
    		return uploadingEntry;
        
        return findOldestEntryWithStatus(
                spoolDirectories.getPending(),
                RecordingSpoolStatus.PENDING
        );
	}
	
	private Optional<RecordingSpoolEntry> findOldestEntryWithStatus(
            Path directory,
            RecordingSpoolStatus requiredStatus
            )
    {
        if(directory == null)
            throw new IllegalArgumentException("Directory cannot be null");

        if(requiredStatus == null)
            throw new IllegalArgumentException("RequiredStatus cannot be null");
        
        Instant currentTime = Instant.now();

        List<RecordingSpoolEntry> entries =
                metadataStore.loadAll(directory);

        return entries.stream()
        		.filter(entry ->
        				entry.getStatus() == requiredStatus)
        		.filter(entry ->
        				requiredStatus
        						!= RecordingSpoolStatus.UPLOAD_CONFIRMED
        				|| entry.isCleanupRetryReady(currentTime))
        		.min(
        				Comparator.comparing(
        						RecordingSpoolEntry::getCreatedAt
        				)
        		);
    }
	
	private void processRecording(RecordingSpoolEntry recordingEntry)
	{
		if(recordingEntry==null)
			throw new IllegalArgumentException("RecordingEntry cannot be null");
		
		if(recordingEntry.getStatus()==RecordingSpoolStatus.PENDING)
        {
            uploadPendingRecording(recordingEntry);
            return;
        }
		
		if(recordingEntry.getStatus()==RecordingSpoolStatus.UPLOADING)
		{
			processUploadingRecording(recordingEntry);
			return;
		}
		
		if(recordingEntry.getStatus()==RecordingSpoolStatus.UPLOAD_CONFIRMED)
		{
			completeConfirmedRecording(recordingEntry);
			return;
		}

		throw new IllegalArgumentException("RecordingEntry must have status PENDING, "
                        + "UPLOADING, or UPLOAD_CONFIRMED"
        );
	}
	
	private void uploadPendingRecording(RecordingSpoolEntry pendingEntry)
	{
		if(pendingEntry==null)
			throw new IllegalArgumentException("PendingEntry cannot be null");
		
		if(pendingEntry.getStatus()!=RecordingSpoolStatus.PENDING)
			throw new IllegalArgumentException("PendingEntry must have status PENDING");
		
		RecordingSpoolEntry uploadingEntry=null;
		
		try
		{
			uploadingEntry=spoolService.moveToUploading(pendingEntry);
		}catch(Exception ex)
		{
			throw new RecordingSpoolException("Moving to uploading failed", ex);
		}
		
		uploadAndCompleteRecording(uploadingEntry);
	}
	
	private void processUploadingRecording(RecordingSpoolEntry uploadingEntry)
	{
		if(uploadingEntry==null)
			throw new IllegalArgumentException("UploadingEntry cannot be null");

		if(uploadingEntry.getStatus()!=RecordingSpoolStatus.UPLOADING)
		{
			throw new IllegalArgumentException("UploadingEntry must have status UPLOADING");
		}

		uploadAndCompleteRecording(uploadingEntry);
	}
	
	private void uploadAndCompleteRecording(RecordingSpoolEntry uploadingEntry)
	{
		if(uploadingEntry==null)
			throw new IllegalArgumentException("UploadingEntry cannot be null");
		
		if(uploadingEntry.getStatus()!=RecordingSpoolStatus.UPLOADING)
			throw new IllegalArgumentException("UploadingEntry must have status UPLOADING");
		
		try
		{
			uploadRecording(uploadingEntry);
		}catch(Exception ex)
		{
			handleUploadFailure(uploadingEntry, ex);
			return;
		}
		
		RecordingSpoolEntry confirmedEntry;
		
		try
		{
			confirmedEntry=spoolService.markUploadConfirmed(uploadingEntry);
		}
		catch(Exception ex)
		{
			throw new RecordingSpoolException(
	                "Upload succeeded, but persisting upload confirmation failed"
	                + " for segment "+uploadingEntry.getSegmentId(), ex);
		}
		
		completeConfirmedRecording(confirmedEntry);
	}
	
	private void uploadRecording(RecordingSpoolEntry uploadingEntry) throws IOException
	{
		if(uploadingEntry==null)
			throw new IllegalArgumentException("UploadingEntry cannot be null");
		
		if(uploadingEntry.getStatus()!=RecordingSpoolStatus.UPLOADING)
			throw new IllegalArgumentException("UploadingEntry must have status UPLOADING");
		
		uploadClient.uploadRecordingSegment(
				uploadingEntry.getDeviceId(),
				uploadingEntry.getSegmentId(),
				apiKey, 
				Files.readAllBytes(uploadingEntry.getVideoPath()), 
				uploadingEntry.getSegmentStartTime(), 
				uploadingEntry.getSegmentEndTime()
				);
	}
	
	private void completeConfirmedRecording(RecordingSpoolEntry confirmedEntry)
	{
		if(confirmedEntry==null)
			throw new IllegalArgumentException("ConfirmedEntry cannot be null");
		
		if(confirmedEntry.getStatus()!=RecordingSpoolStatus.UPLOAD_CONFIRMED)
        {
            throw new IllegalArgumentException("ConfirmedEntry must have status "
            		+ "UPLOAD_CONFIRMED"
            );
        }
		
		try
		{
			if(retainUploadedRecordings)
			{
				spoolService.moveToUploaded(confirmedEntry);
	      	}
	      	else
	       	{
	      		spoolService.deleteEntry(confirmedEntry);
	      	}
	  	}
	 	catch(Exception ex)
	  	{
	 		int nextCleanupAttempt =
					confirmedEntry.getCleanupAttempts() + 1;

			int exponent = Math.min(
					nextCleanupAttempt - 1,
					20
			);

			long delaySeconds = Math.min(
					CLEANUP_MAX_DELAY_SECONDS,
					CLEANUP_BASE_DELAY_SECONDS
							* (1L << exponent)
			);

			Instant nextAttemptAt =
					Instant.now().plusSeconds(delaySeconds);

			RecordingSpoolEntry updatedEntry =
					confirmedEntry.recordCleanupFailure(
							getFailureMessage(ex),
							nextAttemptAt
					);

			metadataStore.save(updatedEntry);

			System.err.println(
					"Recording cleanup failed for segment "
							+ confirmedEntry.getSegmentId()
							+ ". Cleanup attempt "
							+ nextCleanupAttempt
							+ " will run after "
							+ nextAttemptAt);
	  	}
	}
	
	private void handleUploadFailure(RecordingSpoolEntry uploadingEntry, Exception failure)
	{
		if(uploadingEntry==null)
			throw new IllegalArgumentException("UploadingEntry cannot be null");
		
		if(failure == null)
		    throw new IllegalArgumentException("Failure cannot be null");
		
		int attemptsAfterFailure=uploadingEntry.getUploadAttempts()+1;
		String failureMessage = getFailureMessage(failure);
		
		if(attemptsAfterFailure>=maximumUploadAttempts)
			spoolService.moveToFailed(uploadingEntry, failureMessage);
		else
			spoolService.returnToPending(uploadingEntry, failureMessage);
	}
	
	private String getFailureMessage(Exception failure)
	{
		if(failure == null)
		    throw new IllegalArgumentException("Failure cannot be null");
		
		String failureMessage=failure.getLocalizedMessage();
		
		if(failureMessage==null || failureMessage.isBlank())
			failureMessage="Recording Upload Failed";
		
		return failureMessage;
	}
	
	private void waitForRetryInterval()
	{
		 try
		    {
		        Thread.sleep(retryInterval.toMillis());
		    }
		    catch(InterruptedException ex)
		    {
		        running = false;
		        Thread.currentThread().interrupt();
		    }
	}
	
	private void awaitWorkerTermination(Thread thread)
	{
	    if(thread == null || thread == Thread.currentThread())
	        return;

	    try
	    {
	        thread.join();
	    }
	    catch(InterruptedException ex)
	    {
	        Thread.currentThread().interrupt();
	    }
	}
}