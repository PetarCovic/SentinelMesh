package com.sentinelmesh.recordings;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecordingSegmentRepository extends JpaRepository<RecordingSegment, UUID>
{
	@Query("""
		       SELECT recording
		       FROM RecordingSegment recording
		       WHERE recording.device.id = :deviceId
		         AND recording.segmentStartTime < :end
		         AND recording.segmentEndTime > :start
		       ORDER BY recording.segmentStartTime ASC
		       """)
		List<RecordingSegment> findOverlappingSegments(
		        @Param("deviceId") UUID deviceId,
		        @Param("start") Instant start,
		        @Param("end") Instant end
		);
	
	List<RecordingSegment> findTop20ByDeviceIdOrderBySegmentStartTimeDesc(UUID deviceId);
	
	List<RecordingSegment> findByDeviceIdOrderBySegmentStartTimeDesc(UUID deviceId);

	List<RecordingSegment> findBySegmentEndTimeBefore(Instant cutoff);
	
	Optional<RecordingSegment> findBySegmentId(UUID segmentId);
	
	boolean existsBySegmentId(UUID segmentId);
	
	@Query("""
		       SELECT 
		       		MIN(recording.segmentStartTime) AS earliestRecordingTime, 
		       		MAX(recording.segmentEndTime) AS latestRecordingTime, 
		       		COUNT(recording) AS totalSegmentCount, 
		       		COALESCE(SUM(recording.durationSeconds), 0) AS totalRecordedDurationSeconds
		       FROM RecordingSegment recording
		       WHERE recording.device.id = :deviceId
		       """)
	CameraRecordingTimelineAggregate getCameraRecordingTimelineAggregate(
			@Param("deviceId") UUID deviceId
			);
	
	@Query("""
			SELECT 
				recording.device.id as deviceId,
				MIN(recording.segmentStartTime) AS earliestRecordingTime,
				MAX(recording.segmentEndTime) AS latestRecordingTime,
				COUNT(recording) AS recordingSegmentCount
			FROM RecordingSegment recording
			GROUP BY recording.device.id	
			""")
	List<CameraRecordingAggregate> getCameraRecordingAggregates();
	
	@Query(
		    value = """
		        SELECT recording
		        FROM RecordingSegment recording
		        WHERE recording.device.id = :deviceId
		          AND recording.segmentStartTime < :end
		          AND recording.segmentEndTime > :start
		        ORDER BY recording.segmentStartTime ASC, recording.id ASC
		        """,
		    countQuery = """
		        SELECT COUNT(recording)
		        FROM RecordingSegment recording
		        WHERE recording.device.id = :deviceId
		          AND recording.segmentStartTime < :end
		          AND recording.segmentEndTime > :start
		        """
		)
		Page<RecordingSegment> findOverlappingSegmentsPage(
		        @Param("deviceId") UUID deviceId,
		        @Param("start") Instant start,
		        @Param("end") Instant end,
		        Pageable pageable
		);
}