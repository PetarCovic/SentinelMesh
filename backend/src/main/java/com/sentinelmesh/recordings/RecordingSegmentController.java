package com.sentinelmesh.recordings;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class RecordingSegmentController 
{
	private final RecordingSegmentService recordingSegmentService;
	
	public RecordingSegmentController(RecordingSegmentService recordingSegmentService)
	{
		this.recordingSegmentService=recordingSegmentService;
	}
	
	@PostMapping(
			value="/devices/{deviceId}/recordings/segments",
			consumes=MediaType.MULTIPART_FORM_DATA_VALUE
			)
	public ResponseEntity<RecordingSegmentResponse> uploadRecordingSegment(
			@PathVariable UUID deviceId, 
			@RequestParam("file") MultipartFile file,
			@RequestParam("segmentId") UUID segmentId,
			@RequestParam("segmentStartTime") Instant segmentStartTime,
			@RequestParam("segmentEndTime") Instant segmentEndTime
			)
	{
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(recordingSegmentService.uploadRecordingSegment(
						deviceId, 
						segmentId,
						file,  
						segmentStartTime, 
						segmentEndTime
						));
	}
	
	@GetMapping(value="/recordings/segments/{segmentId}")
	public ResponseEntity<RecordingSegmentResponse> getRecordingSegmentMetadata(@PathVariable UUID segmentId)
	{
		return ResponseEntity.ok(recordingSegmentService.getRecordingSegmentMetadata(segmentId));
	}
	
	@GetMapping(value="/recordings/segments/{segmentId}/video")
	public ResponseEntity<Resource> getRecordingSegment(@PathVariable UUID segmentId)
	{
		RecordingSegmentResource segmentResource =
		        recordingSegmentService.loadRecordingSegmentResource(segmentId);
		
		return ResponseEntity.ok()
				.contentType(segmentResource.getMediaType())
				.contentLength(segmentResource.getContentLength())
				.body(segmentResource.getResource());
	}
	
	@GetMapping(value="/devices/{deviceId}/recordings/segments/recent")
	public ResponseEntity<List<RecordingSegmentResponse>> getRecentRecordingSegments(@PathVariable UUID deviceId)
	{
		return ResponseEntity.ok(
			    recordingSegmentService.getRecentSegmentsForDevice(deviceId)
				);
	}
	
	@GetMapping(value="/devices/{deviceId}/recordings/segments")
	public ResponseEntity<CameraRecordingTimelineResponse> getTimeline(
			@PathVariable UUID deviceId,
			@RequestParam("start") Instant start,
			@RequestParam("end") Instant end,
			@RequestParam(name="page", required=false, defaultValue="0") int page,
			@RequestParam(name="size", required=false, defaultValue="25") int size
			)
	{
		return ResponseEntity.ok(recordingSegmentService.getCameraRecordingTimeline(
				deviceId, 
				start, 
				end, 
				page, 
				size
				));
	}
}