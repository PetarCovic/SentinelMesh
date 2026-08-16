package com.sentinelmesh.edge.recording;

import com.sentinelmesh.edge.camera.Frame;
import com.sentinelmesh.edge.client.RecordingSegmentUploadClient;
import com.sentinelmesh.edge.config.EdgeNodeConfig;

public class ContinuousRecordingSubsystem implements AutoCloseable
{
	private final EdgeNodeConfig config;
	private final RecordingSpoolDirectories spoolDirectories;
	private final RecordingSpoolMetadataStore metadataStore;
	private final RecordingSpoolService spoolService;
	private final RecordingSpoolRecoveryService recoveryService;
	private final RecordingSpoolCapacityManager capacityManager;
	private final StreamingRecordingEncoder recordingEncoder;
	private final ContinuousRecordingCoordinator recordingCoordinator;
	private final RecordingUploadWorker uploadWorker;
	private final UploadedRecordingCleanupService cleanupService;
	
	private boolean started;
	private boolean closed;
	
	public ContinuousRecordingSubsystem(
			EdgeNodeConfig config, 
			RecordingSegmentUploadClient uploadClient
			)
	{
		if(config==null)
			throw new IllegalArgumentException("Config cannot be null");
		
		if(uploadClient==null)
			throw new IllegalArgumentException("UploadClient cannot be null");
		
		this.config=config;
		this.spoolDirectories=new RecordingSpoolDirectories(config.getRecordingSpoolRoot());
		this.metadataStore=new RecordingSpoolMetadataStore();
		this.spoolService=new RecordingSpoolService(spoolDirectories, metadataStore);
		this.recoveryService=new RecordingSpoolRecoveryService(
				spoolDirectories, 
				metadataStore,
				spoolService
				);
		this.capacityManager=new RecordingSpoolCapacityManager(
				spoolDirectories,
				metadataStore,
				spoolService,
				config.getRecordingMaximumSpoolSizeBytes(),
				config.getRecordingMinimumFreeDiskSpaceBytes(),
				config.isDeletePendingWhenNecessary()
				);
		this.recordingEncoder=new StreamingRecordingEncoder(
				spoolDirectories,
				config.getDeviceId(),
				Math.toIntExact(config.getRecordingSegmentDurationSeconds()),
				(double)config.getRecordingSegmentFPS()
				);
		this.recordingCoordinator=new ContinuousRecordingCoordinator(
				recordingEncoder,
				spoolService,
				capacityManager
				);
		this.uploadWorker=new RecordingUploadWorker(
				spoolDirectories,
				metadataStore,
				spoolService,
				uploadClient,
				config.getApiKey(),
				config.getRecordingMaximumUploadAttempts(),
				config.getRecordingUploadInterval(),
				config.isRetainUploadedRecordingsEnabled()
				);
		this.cleanupService=new UploadedRecordingCleanupService(
				spoolDirectories,
				metadataStore,
				spoolService,
				config.getUploadedRecordingRetentionDuration(),
				config.getUploadedRecordingCleanupInterval()
				);
		
		this.started=false;
		this.closed=false;
	}
	
	public synchronized void start()
	{		
		if(closed)
			throw new IllegalStateException("ContinuousRecordingSubsystem is closed");
		
		if(started)
			return;
		
		boolean uploadWorkerStarted=false;
		boolean cleanupServiceStarted=false;
		
		try
		{
			recoveryService.recover();
			capacityManager.enforceCapacity();
			
			uploadWorkerStarted=true;
			uploadWorker.start();
			
			if(config.isRetainUploadedRecordingsEnabled())
			{
				cleanupServiceStarted=true;
				cleanupService.start();
			}
			
			started=true;
		}
		catch(Exception startupFailure)
		{			
			try
			{
				if(cleanupServiceStarted)
					cleanupService.close();
			}
			catch(Exception rollbackFailure)
			{
				startupFailure.addSuppressed(rollbackFailure);
			}
			
			try
			{
				if(uploadWorkerStarted)
					uploadWorker.close();
			}
			catch(Exception rollbackFailure)
			{
				startupFailure.addSuppressed(rollbackFailure);
			}
			
			started=false;
			closed=true;
			throw startupFailure;
		}
	}
	
	public synchronized void acceptFrame(Frame frame)
	{
		if(!config.isContinuousRecordingEnabled())
			return;
		
		if(closed)
			throw new IllegalStateException("ContinuousRecordingSubsystem is closed");
		
		if(!started)
			throw new IllegalStateException("ContinuousRecordingSubsystem not started");
		
		if(frame==null || frame.isEmpty())
			throw new IllegalArgumentException("Frame cannot be null or blank");
		
		recordingCoordinator.acceptFrame(frame);
	}
	
	public synchronized void close()
	{
		if(closed)
			return;
		
		started=false;
		
		try
		{
			recordingCoordinator.close();
		}
		catch(Exception ex)
		{
			System.err.println("RecordingCoordinator failed to close: "+ex.getLocalizedMessage());
		}
		
		try
		{
			cleanupService.close();
		}
		catch(Exception ex)
		{
			System.err.println("CleanupService failed to close: "+ex.getLocalizedMessage());
		}
		
		try
		{
			uploadWorker.close();
		}
		catch(Exception ex)
		{
			System.err.println("UploadWorker failed to close: "+ex.getLocalizedMessage());
		}
		
		closed=true;
	}
}