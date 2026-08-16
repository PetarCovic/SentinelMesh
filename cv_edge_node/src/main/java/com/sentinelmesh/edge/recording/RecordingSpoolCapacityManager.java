package com.sentinelmesh.edge.recording;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import com.sentinelmesh.edge.exceptions.RecordingSpoolException;

public class RecordingSpoolCapacityManager 
{
	private final RecordingSpoolDirectories spoolDirectories;
	private final RecordingSpoolMetadataStore metadataStore;
	private final RecordingSpoolService spoolService;

	private final long maximumSpoolSizeBytes;
	private final long minimumFreeDiskSpaceBytes;
	private final boolean deletePendingWhenNecessary;
	
	public RecordingSpoolCapacityManager(
			RecordingSpoolDirectories spoolDirectories,
			RecordingSpoolMetadataStore metadataStore,
			RecordingSpoolService spoolService,
			long maximumSpoolSizeBytes,
			long minimumFreeDiskSpaceBytes,
			boolean deletePendingWhenNecessary
			)
	{
		if(spoolDirectories==null)
			throw new IllegalArgumentException("SpoolDirectories cannot be null");
		
		if(metadataStore==null)
			throw new IllegalArgumentException("MetadataStore cannot be null");
		
		if(spoolService==null)
			throw new IllegalArgumentException("SpoolService cannot be null");
		
		if(maximumSpoolSizeBytes<=0)
			throw new IllegalArgumentException("MaximumSpoolSizeBytes must be greater than 0");
		
		if(minimumFreeDiskSpaceBytes<0)
			throw new IllegalArgumentException("MinimumFreeDiskSpaceBytes cannot be negative");
		
		this.spoolDirectories=spoolDirectories;
		this.metadataStore=metadataStore;
		this.spoolService=spoolService;
		this.maximumSpoolSizeBytes=maximumSpoolSizeBytes;
		this.minimumFreeDiskSpaceBytes=minimumFreeDiskSpaceBytes;
		this.deletePendingWhenNecessary=deletePendingWhenNecessary;
	}
	
	public void enforceCapacity()
	{
		if(!isCapacityExceeded())
			return;
		
		reclaimSpace();
	}

	public boolean isCapacityExceeded()
	{
		return calculateSpoolSizeBytes()>maximumSpoolSizeBytes 
				|| getUsableDiskSpaceBytes()<minimumFreeDiskSpaceBytes;
	}

	public long calculateSpoolSizeBytes()
	{
		Path root=spoolDirectories.getRoot().toAbsolutePath().normalize();
		
		if(!Files.isDirectory(root))
		{
			return 0;
		}
				
		try (Stream<Path> paths = Files.walk(root)) 
		{
			return paths
                .filter(Files::isRegularFile)
                .mapToLong(path ->
                {
                	try
                	{
                		return Files.size(path);
                	}
                	catch (IOException e) 
                	{
                        throw new RecordingSpoolException("Failed to read file size: "+path, e);
                    }
				}).sum();
        } catch (IOException e) {
            throw new RecordingSpoolException("Error calculating file size: ", e);
        }
	}

	private long getUsableDiskSpaceBytes()
	{
		Path root=spoolDirectories.getRoot();
		
		try {
			return Files.getFileStore(root).getUsableSpace();
		} catch (IOException e) 
		{
			throw new RecordingSpoolException("Error getting usable disk space", e);
		}
	}

	private void reclaimSpace()
	{
		while(isCapacityExceeded())
		{
			boolean deleted=deleteOldestUploaded();
			
			if(!deleted)
				deleted=deleteOldestFailed();
			
			if(!deleted && deletePendingWhenNecessary)
				deleted=deleteOldestPending();
		
			if(!deleted)
				throw new RecordingSpoolException("Error reclaiming space");
		}
	}
	
	private boolean deleteOldestInPath(Path path, RecordingSpoolStatus status)
	{		
		if(!Files.isDirectory(path))
		{
			return false;
		}
		
		var metadataFiles=metadataStore.listMetadataFiles(path);
		
		if(metadataFiles.isEmpty())
			return false;
		
		RecordingSpoolEntry oldestEntry=null;
		
		for(Path metadataPath : metadataFiles)
		{
			RecordingSpoolEntry entry;
				
			try
			{
				entry=metadataStore.load(metadataPath);
			}
			catch(Exception ex)
			{
				continue;
			}
				
			if(entry.getStatus()!=status)
				continue;
				
			if(oldestEntry==null || entry.getCreatedAt().isBefore(oldestEntry.getCreatedAt()))
				oldestEntry=entry;
				
		}
		
		if(oldestEntry==null)
			return false;
		
		try
		{
			spoolService.deleteEntry(oldestEntry);
		}
		catch(Exception ex)
		{
			throw new RecordingSpoolException("Failed to delete Entry: "+oldestEntry.getSegmentId()
			, ex);
		}
		
		return true;
	}

	private boolean deleteOldestUploaded()
	{
		Path uploadedPath=spoolDirectories.getUploaded();
		
		return deleteOldestInPath(uploadedPath, RecordingSpoolStatus.UPLOADED);
	}

	private boolean deleteOldestFailed()
	{
		Path failedPath=spoolDirectories.getFailed();
		
		return deleteOldestInPath(failedPath, RecordingSpoolStatus.FAILED);
	}

	private boolean deleteOldestPending()
	{
		Path pendingPath=spoolDirectories.getPending();
		
		return deleteOldestInPath(pendingPath, RecordingSpoolStatus.PENDING);
	}
}
