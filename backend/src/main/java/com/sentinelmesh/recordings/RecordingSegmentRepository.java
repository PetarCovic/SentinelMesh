package com.sentinelmesh.recordings;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
}