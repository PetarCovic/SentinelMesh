package com.sentinelmesh.edge.processing;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.sentinelmesh.edge.camera.Frame;
import com.sentinelmesh.edge.client.SecurityEventClient;
import com.sentinelmesh.edge.config.EdgeNodeConfig;
import com.sentinelmesh.edge.debug.FrameDebugViewer;
import com.sentinelmesh.edge.detection.DetectionPipeline;
import com.sentinelmesh.edge.detection.DetectionResult;
import com.sentinelmesh.edge.detection.DetectionType;
import com.sentinelmesh.edge.detection.PersonDetectionWorker;

public class FrameProcessor 
{
	private final DetectionPipeline detectionPipeline;
	private final PersonDetectionWorker personDetectionWorker;
	private final EdgeNodeConfig config;
	private final DetectionCooldownTracker cooldownTracker;
	private final SecurityEventClient securityEventClient;
	private final FrameDebugViewer debugViewer;
	private final PersonDetectionConfirmationTracker personConfirmationTracker;
	private long lastProcessedPersonResultVersion;
	private boolean latestPersonConfirmed;
	private long lastHandledPersonResultVersion;
	
	private Instant lastPersonDetectionSubmitTime;
	private final Duration personDetectionInterval;
	
	public FrameProcessor(
			DetectionPipeline detectionPipeline,
			PersonDetectionWorker personDetectionWorker,
			EdgeNodeConfig config,
			DetectionCooldownTracker cooldownTracker,
			SecurityEventClient securityEventClient,
			FrameDebugViewer debugViewer
			)
	{
		if(detectionPipeline == null)
			throw new IllegalArgumentException("DetectionPipeline cannot be null");
		
		if(config == null)
			throw new IllegalArgumentException("EdgeNodeConfig cannot be null");
		
		if(cooldownTracker == null)
			throw new IllegalArgumentException("DetectionCooldownTracker cannot be null");
		
		if(securityEventClient == null)
			throw new IllegalArgumentException("SecurityEventClient cannot be null");
		
		this.detectionPipeline = detectionPipeline;
		this.personDetectionWorker = personDetectionWorker;
		this.config = config;
		this.cooldownTracker = cooldownTracker;
		this.securityEventClient = securityEventClient;
		this.debugViewer = debugViewer;
		this.lastPersonDetectionSubmitTime = Instant.EPOCH;
		this.personDetectionInterval = Duration.ofMillis(400);
		this.personConfirmationTracker=new PersonDetectionConfirmationTracker(2, 3);
		this.lastProcessedPersonResultVersion=-1L;
		this.latestPersonConfirmed=false;
		this.lastHandledPersonResultVersion=-1L;
	}
	
	public void process(Frame frame)
	{
		if(frame == null || frame.isEmpty())
			return;
		
		List<DetectionResult> detections = new ArrayList<>();
		
		List<DetectionResult> synchronousDetections = detectionPipeline.detect(frame);
		detections.addAll(synchronousDetections);
		
		if(config.isPersonDetectionEnabled() 
				&& personDetectionWorker!=null
				&& shouldSubmitPersonDetection())
		{			
			boolean submitted=personDetectionWorker.submit(frame);
			
			if(submitted)
				lastPersonDetectionSubmitTime = Instant.now();
		}
		
		List<DetectionResult> personDetections = List.of();
		boolean personConfirmed = false;
		
		if(config.isPersonDetectionEnabled() && personDetectionWorker != null)
		{
			personDetections=personDetectionWorker.getLatestResults();
			
			long currentVersion=personDetectionWorker.getLatestResultVersion();
			
			if(currentVersion>0 && currentVersion!=lastProcessedPersonResultVersion)
			{
				latestPersonConfirmed = personConfirmationTracker.recordAndCheck(personDetections);
				lastProcessedPersonResultVersion = currentVersion;
			}
			
			personConfirmed = latestPersonConfirmed;

			detections.addAll(personDetections);
		}
		
		if(debugViewer != null && debugViewer.isOpen())
			debugViewer.show(frame, detections);
		
		if(detections.isEmpty())
			return;
		
		long currentPersonResultVersion = personDetectionWorker != null
				? personDetectionWorker.getLatestResultVersion()
				: -1L;

		for(DetectionResult detection : detections)
		{
			boolean personDetection = isPersonDetection(detection);
			
			if(personDetection)
			{
				if(!personConfirmed)
					continue;
				
				if(currentPersonResultVersion <= 0)
					continue;
				
				if(currentPersonResultVersion == lastHandledPersonResultVersion)
					continue;
			}
			
			if(cooldownTracker.shouldAllow(detection))
			{
				try
				{
					securityEventClient.sendEvent(config.getDeviceId(), config.getApiKey(), detection);
					cooldownTracker.markSent(detection);
				}
				catch(Exception ex)
				{
					System.out.println("Failed to send security event: " + ex.getMessage());
				}
				finally
				{
					if(personDetection)
						lastHandledPersonResultVersion = currentPersonResultVersion;
				}
			}
		}
	}
	
	private boolean shouldSubmitPersonDetection()
	{
		Duration timeSinceLastSubmit =
				Duration.between(lastPersonDetectionSubmitTime, Instant.now());
		
		return timeSinceLastSubmit.compareTo(personDetectionInterval) >= 0;
	}
	
	private boolean isPersonDetection(DetectionResult detection)
	{
		return detection != null && detection.getType() == DetectionType.PERSON_DETECTED;
	}
}