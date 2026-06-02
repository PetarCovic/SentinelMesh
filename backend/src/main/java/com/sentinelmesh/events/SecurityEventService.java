package com.sentinelmesh.events;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sentinelmesh.async.EventProcessingQueue;
import com.sentinelmesh.common.PageResponse;
import com.sentinelmesh.devices.Device;
import com.sentinelmesh.devices.DeviceAuthenticationService;
import com.sentinelmesh.exceptions.SecurityEventNotFoundException;
import com.sentinelmesh.realtime.DashboardEventBroadcaster;
import com.sentinelmesh.realtime.DashboardEventType;

@Service
public class SecurityEventService 
{
	private final SecurityEventRepository securityEventRepository;
	private final DeviceAuthenticationService deviceAuthenticationService;
	private final EventProcessingQueue eventProcessingQueue;
	private final DashboardEventBroadcaster dashboardEventBroadcaster;
	
	public SecurityEventService(
			SecurityEventRepository securityEventRepository,
			DeviceAuthenticationService deviceAuthenticationService,
			EventProcessingQueue eventProcessingQueue,
			DashboardEventBroadcaster dashboardEventBroadcaster
			)
	{
		this.securityEventRepository=securityEventRepository;
		this.deviceAuthenticationService=deviceAuthenticationService;
		this.eventProcessingQueue=eventProcessingQueue;
		this.dashboardEventBroadcaster=dashboardEventBroadcaster;
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
		
		eventProcessingQueue.enqueue(savedEvent.getId());
		
		dashboardEventBroadcaster.broadcast(
		        DashboardEventType.SECURITY_EVENT_CREATED,
		        Map.of(
		                "eventId", savedEvent.getId().toString(),
		                "deviceId", savedEvent.getDevice().getId().toString(),
		                "deviceName", savedEvent.getDevice().getName(),
		                "eventType", savedEvent.getEventType().toString(),
		                "severity", savedEvent.getSeverity().toString()
		        )
		);
		
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
	
	@Transactional(readOnly = true)
	public PageResponse<SecurityEventResponse> getRecentEvents(int page, int size) {
	    int safePage = Math.max(page, 0);
	    int safeSize = Math.min(Math.max(size, 1), 100);

	    Pageable pageable = PageRequest.of(safePage, safeSize);

	    Page<SecurityEventResponse> responsePage = securityEventRepository
	            .findAllByOrderByReceivedAtDesc(pageable)
	            .map(SecurityEventResponse::from);

	    return PageResponse.from(responsePage);
	}
}
