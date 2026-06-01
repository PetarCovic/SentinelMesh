package com.sentinelmesh.simulator;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Random;

public class EventClient 
{
	private static final String[] EVENT_TYPES=
	{
		"MOTION_DETECTED",
		"PERSON_DETECTED",
		"DOOR_OPENED",
		"SOUND_DETECTED"
	};
	
	private static final String[] SEVERITIES=
	{
		"LOW",
		"MEDIUM",
		"HIGH",
		"CRITICAL"
	};
	
	private final String baseUrl;
	private final HttpJsonClient httpJsonClient;
	private final Random random;
	
	public EventClient(String baseUrl, HttpJsonClient httpJsonClient)
	{
		this.baseUrl=baseUrl;
		this.httpJsonClient=httpJsonClient;
		this.random=new Random();
	}
	
	public SentEvent sendRandomEvent(SimulatedDevice device) throws IOException, InterruptedException 
	{
        String eventType = EVENT_TYPES[random.nextInt(EVENT_TYPES.length)];
        String severity = SEVERITIES[random.nextInt(SEVERITIES.length)];
        double confidence = 0.50 + random.nextDouble() * 0.49;

        String metadataJson = String.format(
                "{\\\"simulated\\\":true,\\\"zone\\\":\\\"%s\\\",\\\"source\\\":\\\"phase8-simulator\\\"}",
                device.location()
        );

        String json = String.format("""
                {
                  "eventType": "%s",
                  "severity": "%s",
                  "confidence": %.2f,
                  "occurredAt": "%s",
                  "metadataJson": "%s"
                }
                """, eventType, severity, confidence, Instant.now(), metadataJson);

        String url = baseUrl + "/api/devices/" + device.id() + "/events";

        httpJsonClient.postJson(
                url,
                json,
                Map.of("X-Device-Api-Key", device.apiKey())
        );

        return new SentEvent(eventType, severity, confidence);
    }
	
	public record SentEvent(String eventType, String severity, double confidence)
	{
		
	}
}
