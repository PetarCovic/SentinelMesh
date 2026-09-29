package com.sentinelmesh.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sentinelmesh.clips.VideoClip;
import com.sentinelmesh.snapshots.Snapshot;

public class SecurityEventResponse 
{
	private UUID id;
	private UUID deviceId;
	private UUID snapshotId;
	private UUID videoClipId;
	private String deviceName;
	private SecurityEventType eventType;
	private SecurityEventSeverity severity;
	private Double confidence;
	private Instant occurredAt;
	private Instant receivedAt;
	private String metadataJson;
	private String snapshotImageUrl;
	private String videoClipUrl;
	private boolean snapshotAvailable;
	private boolean videoClipAvailable;
	
	public SecurityEventResponse(
			UUID id,
			UUID deviceId,
			UUID snapshotId,
			UUID videoClipId,
			String deviceName,
			SecurityEventType eventType,
			SecurityEventSeverity severity,
			Double confidence,
			Instant occurredAt,
			Instant receivedAt,
			String metadataJson,
			String snapshotImageUrl,
			String videoClipUrl,
			boolean snapshotAvailable,
			boolean videoClipAvailable
			)
	{
		this.id=id;
		this.deviceId=deviceId;
		this.snapshotId=snapshotId;
		this.videoClipId=videoClipId;
		this.deviceName=deviceName;
		this.eventType=eventType;
		this.severity=severity;
		this.confidence=confidence;
		this.occurredAt=occurredAt;
		this.receivedAt=receivedAt;
		this.metadataJson=metadataJson;
		this.snapshotImageUrl=snapshotImageUrl;
		this.videoClipUrl=videoClipUrl;
		this.snapshotAvailable=snapshotAvailable;
		this.videoClipAvailable=videoClipAvailable;
	}
	
	public static SecurityEventResponse from(SecurityEvent event)
	{
		return new SecurityEventResponse(
				event.getId(),
				event.getDevice().getId(),
				null,
				null,
				event.getDevice().getName(),
				event.getEventType(),
				event.getSeverity(),
				event.getConfidence(),
				event.getOccurredAt(),
				event.getReceivedAt(),
				event.getMetadataJson(),
				null,
				null,
				false,
				false
				);
	}
	
	public static SecurityEventResponse from(SecurityEvent event, Snapshot snapshot)
	{
		if(snapshot==null)
			return from(event);
		
		return new SecurityEventResponse(
				event.getId(),
				event.getDevice().getId(),
				snapshot.getId(),
				null,
				event.getDevice().getName(),
				event.getEventType(),
				event.getSeverity(),
				event.getConfidence(),
				event.getOccurredAt(),
				event.getReceivedAt(),
				event.getMetadataJson(),
				"/api/snapshots/"+snapshot.getId()+"/image",
				null,
				true,
				false
				);
	}
	
	public static SecurityEventResponse from(SecurityEvent event, VideoClip videoClip)
	{
		if(videoClip==null)
			return from(event);
		
		return new SecurityEventResponse(
				event.getId(),
				event.getDevice().getId(),
				null,
				videoClip.getId(),
				event.getDevice().getName(),
				event.getEventType(),
				event.getSeverity(),
				event.getConfidence(),
				event.getOccurredAt(),
				event.getReceivedAt(),
				event.getMetadataJson(),
				null,
				"/api/clips/"+videoClip.getId()+"/video",
				false,
				true
				);
	}
	
	public static SecurityEventResponse from(SecurityEvent event, Snapshot snapshot, VideoClip videoClip)
	{
		if(snapshot==null && videoClip==null)
			return from(event);
		
		if(snapshot==null)
			return from(event, videoClip);
		
		if(videoClip==null)
			return from(event, snapshot);
		
		return new SecurityEventResponse(
				event.getId(),
				event.getDevice().getId(),
				snapshot.getId(),
				videoClip.getId(),
				event.getDevice().getName(),
				event.getEventType(),
				event.getSeverity(),
				event.getConfidence(),
				event.getOccurredAt(),
				event.getReceivedAt(),
				event.getMetadataJson(),
				"/api/snapshots/"+snapshot.getId()+"/image",
				"/api/clips/"+videoClip.getId()+"/video",
				true,
				true
				);
	}
	
	public static List<SecurityEventResponse> from(List<SecurityEvent> events)
	{
		if(events==null)
			throw new IllegalArgumentException("Events cannot be null");
		
		return events.stream()
				.map(SecurityEventResponse::from)
				.toList();
	}
	
	public UUID getId()
	{
		return id;
	}
	
	public UUID getDeviceId()
	{
		return deviceId;
	}
	
	public UUID getSnapshotId()
	{
		return snapshotId;
	}
	
	public UUID getVideoClipId()
	{
		return videoClipId;
	}
	
	public String getDeviceName()
	{
		return deviceName;
	}
	
	public SecurityEventType getEventType()
	{
		return eventType;
	}
	
	public SecurityEventSeverity getSeverity()
	{
		return severity;
	}
	
	public Double getConfidence()
	{
		return confidence;
	}
	
	public Instant getOccurredAt()
	{
		return occurredAt;
	}
	
	public Instant getReceivedAt()
	{
		return receivedAt;
	}
	
	public String getMetadataJson()
	{
		return metadataJson;
	}
	
	public String getSnapshotImageUrl()
	{
		return snapshotImageUrl;
	}
	
	public String getVideoClipUrl()
	{
		return videoClipUrl;
	}
	
	public boolean isSnapshotAvailable()
	{
		return snapshotAvailable;
	}
	
	public boolean isVideoClipAvailable()
	{
		return videoClipAvailable;
	}
}