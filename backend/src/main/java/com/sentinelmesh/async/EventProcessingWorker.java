package com.sentinelmesh.async;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Component;

import com.sentinelmesh.events.SecurityEvent;
import com.sentinelmesh.events.SecurityEventRepository;
import com.sentinelmesh.rules.RuleEvaluationService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
public class EventProcessingWorker 
{
	private final EventProcessingQueue eventProcessingQueue;
	private final SecurityEventRepository securityEventRepository;
	private final RuleEvaluationService ruleEvaluationService;
	private final EventProcessingProperties properties;
	
	private final ExecutorService executorService=Executors.newSingleThreadExecutor();
	
	private volatile boolean running=true;
	
	public EventProcessingWorker(
			EventProcessingQueue eventProcessingQueue,
			SecurityEventRepository securityEventRepository,
			RuleEvaluationService ruleEvaluationService,
			EventProcessingProperties properties
			)
	{
		this.eventProcessingQueue=eventProcessingQueue;
		this.securityEventRepository=securityEventRepository;
		this.ruleEvaluationService=ruleEvaluationService;
		this.properties=properties;
	}
	
	@PostConstruct
	public void start()
	{
		if(!properties.isEnabled())
			return;
		
		executorService.submit(this::processLoop);
	}
	
	@PreDestroy
	public void stop()
	{
		running=false;
		executorService.shutdownNow();
	}
	
	private void processLoop()
	{
		while(running)
		{
			try
			{
				Optional<UUID> eventId=eventProcessingQueue.dequeue();
				
				eventId.ifPresent(this::processEvent);
			} catch(Exception ex)
			{
				System.err.println("Event processing worker error: "+ex.getMessage());
			}
		}
	}
	
	public void processEvent(UUID securityEventId)
	{
		SecurityEvent event=securityEventRepository.findById(securityEventId).orElse(null);
		
		if(event==null)
		{
			System.err.println("Queued security event not found: "+securityEventId);
			return;
		}
		
		ruleEvaluationService.evaluate(event);
	}
}
