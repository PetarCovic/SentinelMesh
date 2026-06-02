package com.sentinelmesh.alerts;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sentinelmesh.common.PageResponse;
import com.sentinelmesh.events.SecurityEvent;
import com.sentinelmesh.events.SecurityEventSeverity;
import com.sentinelmesh.exceptions.AlertNotFoundException;
import com.sentinelmesh.realtime.DashboardEventBroadcaster;
import com.sentinelmesh.realtime.DashboardEventType;

@Service
public class AlertService 
{
	private final AlertRepository alertRepository;
	private final DashboardEventBroadcaster dashboardEventBroadcaster;
	
	public AlertService(
			AlertRepository alertRepository,
			DashboardEventBroadcaster dashboardEventBroadcaster
			)
	{
		this.alertRepository=alertRepository;
		this.dashboardEventBroadcaster=dashboardEventBroadcaster;
	}
	
	@Transactional
	public void createAlertIfNeeded(SecurityEvent event)
	{
		if(!shouldCreateAlert(event))
			return;
	
	
		if(alertRepository.existsBySecurityEventId(event.getId()))
			return;
	
		Alert alert=new Alert(
				event,
				mapSeverity(event.getSeverity()),
				buildTitle(event),
				buildMessage(event)
				);
		
		alertRepository.save(alert);
	}
	
	@Transactional
	public List<AlertResponse> getAllAlerts()
	{
		return alertRepository
				.findAll()
				.stream()
				.map(AlertResponse::from)
				.toList();
	}
	
	@Transactional(readOnly=true)
	public AlertResponse getAlertById(UUID id)
	{
		Alert alert=alertRepository.findById(id)
				.orElseThrow(()-> new AlertNotFoundException(id));
		
		return AlertResponse.from(alert);
	}
	
	@Transactional(readOnly=true)
	public List<AlertResponse> getOpenAlerts()
	{
		return alertRepository.findByStatus(AlertStatus.OPEN)
				.stream()
				.map(AlertResponse::from)
				.toList();
	}
	
	@Transactional(readOnly = true)
	public List<AlertResponse> getOpenAlertsBySeverity(AlertSeverity severity) {
	    return alertRepository.findByStatusAndSeverity(AlertStatus.OPEN, severity)
	            .stream()
	            .map(AlertResponse::from)
	            .toList();
	}
	
	@Transactional
	public AlertResponse acknowledgeAlert(UUID id)
	{
		Alert alert=alertRepository.findById(id)
				.orElseThrow(() -> new AlertNotFoundException(id));
		
		alert.acknowledge();
		
		dashboardEventBroadcaster.broadcast(
		        DashboardEventType.ALERT_ACKNOWLEDGED,
		        Map.of(
		                "alertId", alert.getId().toString(),
		                "status", alert.getStatus().toString()
		        )
		);
		
		return AlertResponse.from(alert);
	}
	
	@Transactional
	public AlertResponse resolveAlert(UUID id)
	{
		Alert alert=alertRepository.findById(id)
				.orElseThrow(() -> new AlertNotFoundException(id));
		
		alert.resolve();
		
		dashboardEventBroadcaster.broadcast(
		        DashboardEventType.ALERT_RESOLVED,
		        Map.of(
		                "alertId", alert.getId().toString(),
		                "status", alert.getStatus().toString()
		        )
		);
		
		return AlertResponse.from(alert);
	}
	
	private boolean shouldCreateAlert(SecurityEvent event)
	{
		return event.getSeverity()==SecurityEventSeverity.HIGH
				|| event.getSeverity()==SecurityEventSeverity.CRITICAL;
	}
	
	private AlertSeverity mapSeverity(SecurityEventSeverity eventSeverity)
	{
		return switch(eventSeverity)
				{
		case LOW -> AlertSeverity.LOW;
		case MEDIUM -> AlertSeverity.MEDIUM;
		case HIGH -> AlertSeverity.HIGH;
		case CRITICAL -> AlertSeverity.CRITICAL;
				};
	}
	
	private String buildTitle(SecurityEvent event)
	{
		return event.getSeverity()+" security event: "+event.getEventType();
	}
	
	private String buildMessage(SecurityEvent event)
	{
		return "Device "
				+event.getDevice().getName()
				+" reported "
				+event.getEventType()
				+" with severity "
				+event.getSeverity()
				+".";
	}
	
	@Transactional(readOnly = true)
	public PageResponse<AlertResponse> getRecentAlerts(int page, int size) {
	    int safePage = Math.max(page, 0);
	    int safeSize = Math.min(Math.max(size, 1), 100);

	    Pageable pageable = PageRequest.of(safePage, safeSize);

	    Page<AlertResponse> responsePage = alertRepository
	            .findAllByOrderByCreatedAtDesc(pageable)
	            .map(AlertResponse::from);

	    return PageResponse.from(responsePage);
	}

	@Transactional(readOnly = true)
	public PageResponse<AlertResponse> getOpenAlertsPaged(int page, int size) {
	    int safePage = Math.max(page, 0);
	    int safeSize = Math.min(Math.max(size, 1), 100);

	    Pageable pageable = PageRequest.of(safePage, safeSize);

	    Page<AlertResponse> responsePage = alertRepository
	            .findByStatusOrderByCreatedAtDesc(AlertStatus.OPEN, pageable)
	            .map(AlertResponse::from);

	    return PageResponse.from(responsePage);
	}
}
