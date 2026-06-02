package com.sentinelmesh.rules;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sentinelmesh.alerts.Alert;
import com.sentinelmesh.alerts.AlertRepository;
import com.sentinelmesh.events.SecurityEvent;
import com.sentinelmesh.events.SecurityEventSeverity;
import com.sentinelmesh.realtime.DashboardEventBroadcaster;
import com.sentinelmesh.realtime.DashboardEventType;

@Service
public class RuleEvaluationService 
{
	private final AlertRuleRepository alertRuleRepository;
	private final AlertRepository alertRepository;
	private final DashboardEventBroadcaster dashboardEventBroadcaster;
	
	public RuleEvaluationService(
			AlertRuleRepository alertRuleRepository,
			AlertRepository alertRepository,
			DashboardEventBroadcaster dashboardEventBroadcaster
			)
	{
		this.alertRuleRepository=alertRuleRepository;
		this.alertRepository=alertRepository;
		this.dashboardEventBroadcaster=dashboardEventBroadcaster;
	}
	
	@Transactional
	public void evaluate(SecurityEvent event)
	{
		if(alertRepository.existsBySecurityEventId(event.getId()))
			return;
		
		List<AlertRule> enabledRules=alertRuleRepository.findByEnabledTrue();
		
		for(AlertRule rule : enabledRules)
		{
			if(matches(rule, event))
			{
				Alert alert=new Alert(
						event,
						rule.getAlertSeverity(),
						buildTitle(rule, event),
						buildMessage(rule, event)
						);
				
				Alert savedAlert = alertRepository.save(alert);

				dashboardEventBroadcaster.broadcast(
				        DashboardEventType.ALERT_CREATED,
				        Map.of(
				                "alertId", savedAlert.getId().toString(),
				                "securityEventId", event.getId().toString(),
				                "deviceId", event.getDevice().getId().toString(),
				                "deviceName", event.getDevice().getName(),
				                "severity", savedAlert.getSeverity().toString(),
				                "title", savedAlert.getTitle()
				        )
				);

				return;
			}
		}
	}
	
	public boolean matches(AlertRule rule, SecurityEvent event)
	{
		if(!rule.isEnabled())
			return false;
		
		if(rule.getEventType()!=null && rule.getEventType()!=event.getEventType())
			return false;
		
		if(rule.getMinimumSeverity()!=null 
				&& severityRank(event.getSeverity())<severityRank(rule.getMinimumSeverity()))
			return false;
		
		if(rule.getDeviceType()!=null
				&& rule.getDeviceType()!=event.getDevice().getType())
			return false;
		
		return true;
	}
	
    private String buildTitle(AlertRule rule, SecurityEvent event) 
    {
        return rule.getAlertTitle()
                .replace("{eventType}", event.getEventType().toString())
                .replace("{severity}", event.getSeverity().toString())
                .replace("{deviceName}", event.getDevice().getName());
    }

    private String buildMessage(AlertRule rule, SecurityEvent event) 
    {
        return rule.getAlertMessage()
                .replace("{eventType}", event.getEventType().toString())
                .replace("{severity}", event.getSeverity().toString())
                .replace("{deviceName}", event.getDevice().getName())
                .replace("{deviceLocation}", event.getDevice().getLocation());
    }
    
    private int severityRank(SecurityEventSeverity severity)
    {
    	return switch(severity)
    	{
    		case LOW -> 1;
    		case MEDIUM -> 2;
    		case HIGH -> 3;
    		case CRITICAL -> 4;
    	};
    }
}
