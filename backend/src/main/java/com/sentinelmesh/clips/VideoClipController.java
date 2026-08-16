package com.sentinelmesh.clips;

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
public class VideoClipController 
{
	private final VideoClipService videoClipService;
	
	public VideoClipController(VideoClipService videoClipService)
	{
		this.videoClipService=videoClipService;
	}
	
	@PostMapping(
			value="/devices/{deviceId}/events/{eventId}/clip",
			consumes=MediaType.MULTIPART_FORM_DATA_VALUE
			)
	public ResponseEntity<VideoClipResponse> uploadVideoClip(
			@PathVariable UUID deviceId, 
			@PathVariable UUID eventId,
			@RequestParam("file") MultipartFile file)
	{
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(videoClipService.uploadVideoClip(deviceId, eventId, file));
	}
	
	@GetMapping("/clips/{clipId}")
	public ResponseEntity<VideoClipResponse> getVideoClipMetadata(@PathVariable UUID clipId)
	
	{
		return ResponseEntity.ok(videoClipService.getVideoClipMetadata(clipId));
	}
	
	@GetMapping("/clips/{clipId}/video")
	public ResponseEntity<Resource> getVideoClipVideo(@PathVariable UUID clipId)
	{
		VideoClipResource videoClipResource=videoClipService.loadVideoClipResource(clipId);
		
		return ResponseEntity.ok()
				.contentType(videoClipResource.getMediaType())
				.body(videoClipResource.getResource());
	}
}
