package com.sentinelmesh.recordings;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class RecordingStorageReconciliationService 
{
	private final RecordingSegmentRepository segmentRepository;
	private final RecordingSegmentStorageService storageService;
	private final RecordingReconciliationProperties reconciliationProperties;
	
	public RecordingStorageReconciliationService(
			RecordingSegmentRepository segmentRepository,
			RecordingSegmentStorageService storageService,
			RecordingReconciliationProperties reconciliationProperties
			)
	{
		if(segmentRepository==null)
			throw new IllegalArgumentException("segmentRepository cannot be null");
		
		if(storageService==null)
			throw new IllegalArgumentException("storageService cannot be null");
		
		if(reconciliationProperties==null)
			throw new IllegalArgumentException("reconciliationProperties cannot be null");
	
		this.segmentRepository=segmentRepository;
		this.storageService=storageService;
		this.reconciliationProperties=reconciliationProperties;
	}
	
	public void reconcileStorage()
	{
		if(reconciliationProperties.isEnabled())
		{
			Instant cutoff=Instant.now().minus(reconciliationProperties.getSafetyWindow());
			reconcileMissingFiles(cutoff);
			reconcileOrphanFiles(cutoff);
		}
	}
	
	private int reconcileMissingFiles(Instant cutoff)
	{
		if(cutoff==null)
			throw new IllegalArgumentException("cutoff cannot be null");
		
		List<RecordingSegment> segments =
		        segmentRepository.findByCreatedAtBeforeOrderByCreatedAtAscIdAsc(
		                cutoff,
		                PageRequest.of(0, 100));
	
		int count=0;
		
		while(!segments.isEmpty())
		{
			Instant afterCreatedAt=segments.get(segments.size()-1).getCreatedAt();
			UUID afterId=segments.get(segments.size()-1).getId();
			
			for(int i=0; i<segments.size(); i++)
			{
				if(!storageService.recordingSegmentFileExists(segments.get(i)))
				{
					segmentRepository.delete(segments.get(i));
					count++;
				}
			}
			
			segments=segmentRepository.findReconciliationBatchAfter(
					cutoff, 
					afterCreatedAt, 
					afterId, 
					PageRequest.of(0, 100));
		}
		
		return count;
	}
	
	private int reconcileOrphanFiles(Instant cutoff)
	{
		if(cutoff==null)
			throw new IllegalArgumentException("cutoff cannot be null");
		
		return storageService.deleteUnreferencedRecordingFiles(cutoff, segmentRepository::existsBySegmentId);
	}
}
