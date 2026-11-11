package com.sentinelmesh.recordings;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RecordingReconciliationScheduler 
{
	private final RecordingStorageReconciliationService reconciliationService;
	
	public RecordingReconciliationScheduler(RecordingStorageReconciliationService reconciliationService)
	{
		this.reconciliationService=reconciliationService;
	}
	
	@Scheduled(fixedDelayString="${sentinelmesh.recordings.reconciliation.interval:6h}")
	public void runReconciliation()
	{
		reconciliationService.reconcileStorage();
	}
}
