package com.sentinelmesh.edge.recording;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.sentinelmesh.edge.exceptions.RecordingSpoolException;

public class UploadedRecordingCleanupService implements AutoCloseable
{
	private final RecordingSpoolDirectories spoolDirectories;
	private final RecordingSpoolMetadataStore metadataStore;
	private final RecordingSpoolService spoolService;

	private final Duration uploadedRetentionDuration;
	private final Duration cleanupInterval;
	
	private final ScheduledExecutorService scheduler;
	
	private boolean started;
	private boolean closed;
	
	public UploadedRecordingCleanupService(
			RecordingSpoolDirectories spoolDirectories,
			RecordingSpoolMetadataStore metadataStore,
			RecordingSpoolService spoolService,
			Duration uploadedRetentionDuration,
			Duration cleanupInterval
			)
	{
		if(spoolDirectories==null)
			throw new IllegalArgumentException("SpoolDirectories cannot be null");
		
		if(metadataStore==null)
			throw new IllegalArgumentException("MetadataStore cannot be null");
		
		if(spoolService==null)
			throw new IllegalArgumentException("SpoolService cannot be null");
		
		if(uploadedRetentionDuration==null)
			throw new IllegalArgumentException("UploadedRetentionDuration cannot be null");
		
		if(uploadedRetentionDuration.isNegative())
			throw new IllegalArgumentException("UploadedRetentionDuration cannot be negative");
		
		if(cleanupInterval==null)
			throw new IllegalArgumentException("CleanupInterval cannot be null");
		
		if(!cleanupInterval.isPositive())
			throw new IllegalArgumentException("Cleanup interval must be greater than 0");
		
		if(cleanupInterval.compareTo(Duration.ofSeconds(1)) < 0)
			throw new IllegalArgumentException("CleanupInterval must be at least 1 second");
		
		this.spoolDirectories=spoolDirectories;
		this.metadataStore=metadataStore;
		this.spoolService=spoolService;
		this.uploadedRetentionDuration=uploadedRetentionDuration;
		this.cleanupInterval=cleanupInterval;
		this.scheduler=Executors.newScheduledThreadPool(1);
		this.started=false;
		this.closed=false;
	}
	
	public synchronized void start()
	{
		if(closed)
			throw new IllegalArgumentException("Thread is closed");
		
		if(started)
			return;
		
		long cleanupIntervalSeconds=cleanupInterval.toSeconds();
		
		this.scheduler.scheduleWithFixedDelay(() ->
		{
			runCleanup();
		}, 0, cleanupIntervalSeconds, TimeUnit.SECONDS);
		
		started=true;
	}
	
	public void runCleanup()
	{
		try
		{
			List<Path> metadataPath=metadataStore.listMetadataFiles(spoolDirectories.getUploaded());
			
			Instant now=Instant.now();
			for(Path path : metadataPath)
			{
				RecordingSpoolEntry entry;
				
				try
				{
					entry=metadataStore.load(path);
					
					if(entry.getStatus()!=RecordingSpoolStatus.UPLOADED)
						continue;
					
					if(isExpired(entry, now))
						deleteExpiredEntry(entry);
				}
				catch(Exception ex)
				{
					System.err.println(path+"\n"+ex.getLocalizedMessage());
					continue;
				}
			}
		}
		catch(Exception ex)
		{
			System.err.println(ex.getLocalizedMessage());
		}
	}
	
	@Override
	public synchronized void close()
	{
		if(closed)
			return;
		
		started=false;
		
		try {
			scheduler.close();
			scheduler.awaitTermination(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) 
		{
			Thread.currentThread().interrupt();
			throw new RecordingSpoolException("Closing cleanup service failed", e);
		}
		
		closed=true;
	}
	
	private boolean isExpired(RecordingSpoolEntry entry, Instant currentTime)
	{
		Instant expiration=entry.getCreatedAt().plus(uploadedRetentionDuration);
		return currentTime.isAfter(expiration)
				|| currentTime.equals(expiration);
	}
	
	private void deleteExpiredEntry(RecordingSpoolEntry entry)
	{
		spoolService.deleteEntry(entry);
	}
}