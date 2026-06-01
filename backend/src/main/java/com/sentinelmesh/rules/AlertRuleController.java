package com.sentinelmesh.rules;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/rules")
public class AlertRuleController 
{
	private final AlertRuleService alertRuleService;
	
	public AlertRuleController(AlertRuleService alertRuleService)
	{
		this.alertRuleService=alertRuleService;
	}
	
	@PostMapping
	public ResponseEntity<AlertRuleResponse> createRule(
			@Valid @RequestBody CreateAlertRuleRequest request
			)
	{
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(alertRuleService.createRule(request));
	}
	
	@GetMapping
	public ResponseEntity<List<AlertRuleResponse>> getAllRules()
	{
		return ResponseEntity.ok(alertRuleService.getAllRules());
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<AlertRuleResponse> getRuleById(@PathVariable UUID id)
	{
		return ResponseEntity.ok(alertRuleService.getRuleById(id));
	}
	
	@PatchMapping("/{id}")
	public ResponseEntity<AlertRuleResponse> updateRule(
			@PathVariable UUID id,
			@Valid @RequestBody UpdateAlertRuleRequest request
			)
	{
		return ResponseEntity.ok(alertRuleService.updateRule(id, request));
	}
	
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteRule(@PathVariable UUID id)
	{
		alertRuleService.deleteRule(id);
		
		return ResponseEntity.noContent().build();
	}
	
	@PatchMapping("/{id}/enable")
	public ResponseEntity<AlertRuleResponse> enableRule(@PathVariable UUID id)
	{
		return ResponseEntity.ok(alertRuleService.enableRule(id));
	}
	
	@PatchMapping("/{id}/disable")
	public ResponseEntity<AlertRuleResponse> disableRule(@PathVariable UUID id)
	{
		return ResponseEntity.ok(alertRuleService.disableRule(id));
	}
}
