package com.sentinelmesh.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sentinelmesh.alerts.AlertService;
import com.sentinelmesh.devices.Device;
import com.sentinelmesh.devices.DeviceAuthenticationService;
import com.sentinelmesh.exceptions.SecurityEventNotFoundException;

@Service
public class SecurityEventService 
{
	private final SecurityEventRepository securityEventRepository;
	private final DeviceAuthenticationService deviceAuthenticationService;
	private final AlertService alertService;
	
	public SecurityEventService(
			SecurityEventRepository securityEventRepository,
			DeviceAuthenticationService deviceAuthenticationService,
			AlertService alertService
			)
	{
		this.securityEventRepository=securityEventRepository;
		this.deviceAuthenticationService=deviceAuthenticationService;
		this.alertService=alertService;
	}
	
	@Transactional
	public SecurityEventResponse createEvent(
			UUID deviceId,
			String rawApiKey,
			CreateSecurityEventRequest request
			)
	{
		Device device=deviceAuthenticationService.authenticate(deviceId, rawApiKey);
		
		Instant occurredAt=request.getOccurredAt() != null 
				? request.getOccurredAt()
				: Instant.now();
		
		SecurityEvent event=new SecurityEvent(
				device,
				request.getEventType(),
				request.getSeverity(),
				request.getConfidence(),
				occurredAt,
				request.getMetadataJson()
				);
		
		SecurityEvent savedEvent=securityEventRepository.save(event);
		
		alertService.createAlertIfNeeded(savedEvent);
		
		return SecurityEventResponse.from(savedEvent);
	}
	
	@Transactional(readOnly=true)
	public List<SecurityEventResponse> getAllEvents()
	{
		return securityEventRepository
				.findAll()
				.stream()
				.map(SecurityEventResponse::from).toList();
	}
	
	@Transactional(readOnly=true)
	public SecurityEventResponse getEventById(UUID id)
	{
		SecurityEvent event=securityEventRepository
				.findById(id)
				.orElseThrow(() -> new SecurityEventNotFoundException(id));
		
		return SecurityEventResponse.from(event);
	}
	
	@Transactional(readOnly=true)
	public List<SecurityEventResponse> getEventsByDevice(UUID deviceId)
	{
		return securityEventRepository.findByDeviceId(deviceId)
				.stream()
				.map(SecurityEventResponse::from)
				.toList();
	}
}
