package com.sentinelmesh.edge.processing;

import java.util.List;

import com.sentinelmesh.edge.camera.Frame;
import com.sentinelmesh.edge.client.SecurityEventClient;
import com.sentinelmesh.edge.config.EdgeNodeConfig;
import com.sentinelmesh.edge.debug.FrameDebugViewer;
import com.sentinelmesh.edge.detection.DetectionPipeline;
import com.sentinelmesh.edge.detection.DetectionResult;

public class FrameProcessor 
{
	private final DetectionPipeline detectionPipeline;
	private final EdgeNodeConfig config;
	private final DetectionCooldownTracker cooldownTracker;
	private final SecurityEventClient securityEventClient;
	private final FrameDebugViewer debugViewer;
	
	public FrameProcessor(
			DetectionPipeline detectionPipeline,
			EdgeNodeConfig config,
			DetectionCooldownTracker cooldownTracker,
			SecurityEventClient securityEventClient,
			FrameDebugViewer debugViewer
			)
	{
		if(detectionPipeline==null)
			throw new IllegalArgumentException("DetectionPipeline cannot be null");
		
		if(config==null)
			throw new IllegalArgumentException("EdgeNodeConfig cannot be null");
		
		if(cooldownTracker==null)
			throw new IllegalArgumentException("DetectionCooldownTracker cannot be null");
		
		if(securityEventClient==null)
			throw new IllegalArgumentException("SecurityEventClient cannot be null");
		
		this.detectionPipeline=detectionPipeline;
		this.config=config;
		this.cooldownTracker=cooldownTracker;
		this.securityEventClient=securityEventClient;
		this.debugViewer=debugViewer;
	}
	
	public void process(Frame frame)
	{
		if(frame==null || frame.isEmpty())
			throw new IllegalArgumentException("Frame cannot be null or empty");
		
		if(!config.isDetectionEnabled())
		{
			if(debugViewer!=null && debugViewer.isOpen())
				debugViewer.show(frame, List.of());
			return;
		}
		
		List<DetectionResult> detections=detectionPipeline.detect(frame);
		
		if(debugViewer!=null && debugViewer.isOpen())
			debugViewer.show(frame, detections);
		
		if(detections.isEmpty())
			return;
		
		System.out.println("Raw detections: "+detections.size());
		System.out.println();
		for(DetectionResult detection : detections)
		{
			if(cooldownTracker.shouldAllow(detection))
			{
				try
				{
					securityEventClient.sendEvent(config.getDeviceId(), config.getApiKey(), detection);
					cooldownTracker.markSent(detection);
					System.out.println("Security Event Sent");
					System.out.println(detection.toString());
				}
				catch(Exception ex)
				{
					System.out.println("Failed to send security event: "+ex.getMessage());
				}
			}
		}
	}
}