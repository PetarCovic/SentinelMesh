package com.sentinelmesh.events;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
public class SecurityEventController 
{
	private final SecurityEventService securityEventService;
	
	public SecurityEventController(SecurityEventService securityEventService)
	{
		this.securityEventService=securityEventService;
	}
	
	@PostMapping("/api/devices/{deviceId}/events")
	public ResponseEntity<SecurityEventResponse> createEvent(
			@PathVariable UUID deviceId,
			@RequestHeader("X-Device-Api-Key") String apiKey,
			@Valid @RequestBody CreateSecurityEventRequest request
			)
	{
		SecurityEventResponse response=securityEventService.createEvent(
				deviceId, 
				apiKey, 
				request
				);
		
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
	
	@GetMapping("/api/events")
	public ResponseEntity<List<SecurityEventResponse>> getAllEvents()
	{
		return ResponseEntity.ok(securityEventService.getAllEvents());
	}
	
	@GetMapping("/api/events/{id}")
	public ResponseEntity<SecurityEventResponse> getEventById(@PathVariable UUID id)
	{
		return ResponseEntity.ok(securityEventService.getEventById(id));
	}
	
	@GetMapping("/api/devices/{deviceId}/events")
	public ResponseEntity<List<SecurityEventResponse>> getEventsByDevice(
			@PathVariable UUID deviceId
			)
	{
		return ResponseEntity.ok(securityEventService.getEventsByDevice(deviceId));
	}
}
