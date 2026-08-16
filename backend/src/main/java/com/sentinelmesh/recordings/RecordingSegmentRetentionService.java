package com.sentinelmesh.recordings;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class RecordingSegmentRetentionService
{
	private final RecordingSegmentRepository segmentRepository;
	private final RecordingSegmentStorageService storageService;

	public RecordingSegmentRetentionService(
			RecordingSegmentRepository segmentRepository,
			RecordingSegmentStorageService storageService)
	{
		if(segmentRepository==null)
			throw new IllegalArgumentException("SegmentRepository cannot be null");

		if(storageService==null)
			throw new IllegalArgumentException("StorageService cannot be null");

		this.segmentRepository=segmentRepository;
		this.storageService=storageService;
	}

	public int deleteExpiredRecordingSegments(Duration retentionDuration)
	{
		if(retentionDuration==null)
			throw new IllegalArgumentException("RetentionDuration cannot be null");

		if(retentionDuration.isNegative())
			throw new IllegalArgumentException("RetentionDuration cannot be negative");

		Instant cutoff=Instant.now().minus(retentionDuration);

		return deleteRecordingSegmentsEndingBefore(cutoff);
	}

	public int deleteRecordingSegmentsEndingBefore(Instant cutoff)
	{
		if(cutoff==null)
			throw new IllegalArgumentException("Cutoff cannot be null");

		List<RecordingSegment> expiredSegments=segmentRepository.findBySegmentEndTimeBefore(cutoff);

		int deletedCount=0;

		for(RecordingSegment segment : expiredSegments)
		{
			try
			{
				storageService.deleteRecordingSegmentFile(segment);

				segmentRepository.delete(segment);

				deletedCount++;
			}
			catch(Exception ex)
			{
				System.err.println(
						"Failed to delete expired recording segment "
						+ segment.getSegmentId()
						+ ": "
						+ ex.getMessage()
						);
			}
		}

		return deletedCount;
	}
}