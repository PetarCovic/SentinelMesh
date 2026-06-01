package com.sentinelmesh.rules;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sentinelmesh.exceptions.AlertRuleNotFoundException;

@Service
public class AlertRuleService 
{
	private final AlertRuleRepository alertRuleRepository;
	
	public AlertRuleService(AlertRuleRepository alertRuleRepository)
	{
		this.alertRuleRepository=alertRuleRepository;
	}
	
	@Transactional
	public AlertRuleResponse createRule(CreateAlertRuleRequest request)
	{
		AlertRule rule=new AlertRule(
				request.getName(),
				request.getDescription(),
				request.isEnabled(),
				request.getEventType(),
				request.getMinimumSeverity(),
				request.getDeviceType(),
				request.getAlertSeverity(),
				request.getAlertTitle(),
				request.getAlertMessage()
				);
		
		AlertRule savedRule=alertRuleRepository.save(rule);
		
		return AlertRuleResponse.from(savedRule);
	}
	
	@Transactional(readOnly=true)
	public List<AlertRuleResponse> getAllRules()
	{
		return alertRuleRepository.findAll().stream().map(AlertRuleResponse::from).toList();
	}
	
	@Transactional(readOnly=true)
	public AlertRuleResponse getRuleById(UUID id)
	{
		AlertRule rule=alertRuleRepository.findById(id)
				.orElseThrow(() -> new AlertRuleNotFoundException(id));
		
		return AlertRuleResponse.from(rule);
	}
	
	@Transactional
	public AlertRuleResponse updateRule(UUID id, UpdateAlertRuleRequest request)
	{
		AlertRule rule=alertRuleRepository.findById(id)
				.orElseThrow(() -> new AlertRuleNotFoundException(id));
		
		rule.update(
				request.getName(),
				request.getDescription(), 
				request.getEnabled(), 
				request.getEventType(), 
				request.getMinimumSeverity(), 
				request.getDeviceType(), 
				request.getAlertSeverity(), 
				request.getAlertTitle(),
				request.getAlertMessage()
				);
		
		return AlertRuleResponse.from(rule);
	}
	
	@Transactional
	public void deleteRule(UUID id)
	{
		if(!alertRuleRepository.existsById(id))
			throw new AlertRuleNotFoundException(id);
		
		alertRuleRepository.deleteById(id);
	}
	
	@Transactional
	public AlertRuleResponse enableRule(UUID id)
	{
		AlertRule rule=alertRuleRepository.findById(id)
				.orElseThrow(() -> new AlertRuleNotFoundException(id));
		
		rule.enable();
		
		return AlertRuleResponse.from(rule);
	}
	
	@Transactional
	public AlertRuleResponse disableRule(UUID id)
	{
		AlertRule rule=alertRuleRepository.findById(id)
				.orElseThrow(() -> new AlertRuleNotFoundException(id));
		
		rule.disable();
		
		return AlertRuleResponse.from(rule);
	}
}
