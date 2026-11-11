package com.sentinelmesh.recordings;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RecordingRetentionScheduler 
{
	private final RecordingSegmentRetentionService retentionService;
	
	public RecordingRetentionScheduler(RecordingSegmentRetentionService retentionService)
	{
		this.retentionService=retentionService;
	}
	
	@Scheduled(fixedDelayString = "${sentinelmesh.recordings.retention.cleanup-interval:1h}")
	public void runCleanup()
	{
		retentionService.runRetentionCleanup();
	}
}
