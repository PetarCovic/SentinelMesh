package com.sentinelmesh.snapshots;

import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class SnapshotController 
{
	private final SnapshotService snapshotService;
	
	public SnapshotController(SnapshotService snapshotService)
	{
		this.snapshotService=snapshotService;
	}
	
	@PostMapping(
			  value = "/devices/{deviceId}/events/{eventId}/snapshot",
			  consumes = MediaType.MULTIPART_FORM_DATA_VALUE
			)
	public ResponseEntity<SnapshotResponse> uploadSnapshot(
			@PathVariable UUID deviceId,
			@PathVariable UUID eventId,
			@RequestHeader("X-Device-Api-Key") String rawApiKey,
			@RequestPart("file") MultipartFile file
			)
	{
		SnapshotResponse response=
				snapshotService.uploadSnapshotForEvent(deviceId, eventId, rawApiKey, file);
		
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
	
	@GetMapping("/snapshots/{snapshotId}")
	public ResponseEntity<SnapshotResponse> getSnapshotMetadataBySnapshotId(@PathVariable UUID snapshotId)
	{
		return ResponseEntity.ok(snapshotService.getSnapshot(snapshotId));
	}
	
	@GetMapping("/events/{eventId}/snapshot")
	public ResponseEntity<SnapshotResponse> getSnapshotMetadataByEventId(@PathVariable UUID eventId)
	{
		return ResponseEntity.ok(snapshotService.getSnapshotForEvent(eventId));
	}
	
	@GetMapping("/snapshots/{snapshotId}/image")
	public ResponseEntity<Resource> getSnapshotImageBytes(@PathVariable UUID snapshotId)
	{
		SnapshotImageResource image=snapshotService.loadSnapshotImageResource(snapshotId);
		
		return ResponseEntity.ok().contentType(image.getMediaType()).body(image.getResource());
	}
}