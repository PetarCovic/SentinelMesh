package com.sentinelmesh.alerts;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sentinelmesh.common.PageResponse;

@RestController
@RequestMapping("/api/alerts")
public class AlertController 
{
	private final AlertService alertService;
	
	public AlertController(AlertService alertService)
	{
		this.alertService=alertService;
	}
	
	@GetMapping
	public ResponseEntity<List<AlertResponse>> getAllAlerts()
	{
		return ResponseEntity.ok(alertService.getAllAlerts());
	}
	
	@GetMapping("/open")
	public ResponseEntity<PageResponse<AlertResponse>> getOpenAlerts(
	        @RequestParam(defaultValue = "0") int page,
	        @RequestParam(defaultValue = "50") int size
	) {
	    return ResponseEntity.ok(alertService.getOpenAlertsPaged(page, size));
	}
	
	@GetMapping(value="/open", params="severity")
	public ResponseEntity<List<AlertResponse>> getOpenAlertsBySeverity(
			@RequestParam AlertSeverity severity
			)
	{
		return ResponseEntity.ok(alertService.getOpenAlertsBySeverity(severity));
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<AlertResponse> getAlertById(@PathVariable UUID id)
	{
		return ResponseEntity.ok(alertService.getAlertById(id));
	}
	
	@PatchMapping("/{id}/acknowledge")
	public ResponseEntity<AlertResponse> acknowledgeAlert(@PathVariable UUID id)
	{
		return ResponseEntity.ok(alertService.acknowledgeAlert(id));
	}
	
	@PatchMapping("/{id}/resolve")
	public ResponseEntity<AlertResponse> resolveAlert(@PathVariable UUID id)
	{
		return ResponseEntity.ok(alertService.resolveAlert(id));
	}
	
	@GetMapping("/recent")
	public ResponseEntity<PageResponse<AlertResponse>> getRecentAlerts(
	        @RequestParam(defaultValue = "0") int page,
	        @RequestParam(defaultValue = "50") int size
	) {
	    return ResponseEntity.ok(alertService.getRecentAlerts(page, size));
	}
}
