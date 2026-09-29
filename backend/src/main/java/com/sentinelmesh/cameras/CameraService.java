package com.sentinelmesh.cameras;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sentinelmesh.clips.VideoClip;
import com.sentinelmesh.clips.VideoClipRepository;
import com.sentinelmesh.devices.Device;
import com.sentinelmesh.devices.DeviceRepository;
import com.sentinelmesh.devices.DeviceStatus;
import com.sentinelmesh.devices.DeviceType;
import com.sentinelmesh.events.SecurityEvent;
import com.sentinelmesh.events.SecurityEventRepository;
import com.sentinelmesh.events.SecurityEventResponse;
import com.sentinelmesh.exceptions.DeviceNotFoundException;
import com.sentinelmesh.live.LiveFrameData;
import com.sentinelmesh.live.LiveFrameService;
import com.sentinelmesh.recordings.CameraRecordingAggregate;
import com.sentinelmesh.recordings.CameraRecordingTimelineAggregate;
import com.sentinelmesh.recordings.RecordingSegmentRepository;
import com.sentinelmesh.snapshots.Snapshot;
import com.sentinelmesh.snapshots.SnapshotRepository;

@Service
public class CameraService 
{
	private final DeviceRepository deviceRepository;
	private final LiveFrameService liveFrameService;
	private final RecordingSegmentRepository recordingSegmentRepository;
	private final SecurityEventRepository securityEventRepository;
	private final SnapshotRepository snapshotRepository;
	private final VideoClipRepository videoClipRepository;
	
	public CameraService(
			DeviceRepository deviceRepository,
			LiveFrameService liveFrameService,
			RecordingSegmentRepository recordingSegmentRepository,
			SecurityEventRepository securityEventRepository,
			SnapshotRepository snapshotRepository,
			VideoClipRepository videoClipRepository
			)
	{
		if(deviceRepository==null)
			throw new IllegalArgumentException("DeviceRepository cannot be null");
		
		if(liveFrameService==null)
			throw new IllegalArgumentException("LiveFrameService cannot be null");
		
		if(recordingSegmentRepository==null)
			throw new IllegalArgumentException("RecordingSegmentRepository cannot be null");
		
		if(securityEventRepository==null)
			throw new IllegalArgumentException("SecurityEventRepository cannot be null");
		
		if(snapshotRepository==null)
			throw new IllegalArgumentException("SnapshotRepository cannot be null");
		
		if(videoClipRepository==null)
			throw new IllegalArgumentException("VideoClipRepository cannot be null");
		
		this.deviceRepository=deviceRepository;
		this.liveFrameService=liveFrameService;
		this.recordingSegmentRepository=recordingSegmentRepository;
		this.securityEventRepository=securityEventRepository;
		this.snapshotRepository=snapshotRepository;
		this.videoClipRepository=videoClipRepository;
	}
	
	@Transactional(readOnly=true)
	public CameraDetailsResponse getCameraDetails(UUID deviceId)
	{
		if(deviceId==null)
			throw new IllegalArgumentException("DeviceId cannot be null");
		
		Device device=deviceRepository.findById(deviceId).orElseThrow(
			    () -> new DeviceNotFoundException(deviceId));
		
		if(device.getType()!=DeviceType.CAMERA)
			throw new IllegalArgumentException("Device must be a camera device");
		
		CameraLiveState liveState=resolveLiveStatus(device);
				
		List<SecurityEvent> recentEventEntities= 
				securityEventRepository.findTop5ByDeviceIdOrderByReceivedAtDesc(deviceId);
		
		CameraRecordingTimelineAggregate timelineAggregate=
				recordingSegmentRepository.getCameraRecordingTimelineAggregate(deviceId);
		
		CameraRecordingState recordingState=resolveRecordingState(timelineAggregate);
		
		return new CameraDetailsResponse(
				deviceId, 
				device.getName(),
				device.getLocation(),
				device.getStatus(),
				liveState.getLiveStatus(),
				liveState.getLatestLiveFrameTime(),
				recordingState.isRecordingAvailable(),
				recordingState.getRecordingSegmentCount(),
				recordingState.getEarliestRecordingTime(),
				recordingState.getLatestRecordingTime(),				
				toRecentEventResponses(recentEventEntities)
				);
	}
	
	public List<CameraSummaryResponse> getCameraSummaries()
	{
		List<Device> devices=deviceRepository.findByType(DeviceType.CAMERA);
		
		List<CameraRecordingAggregate> recordingAggregates=
				recordingSegmentRepository.getCameraRecordingAggregates();
		
		Map<UUID, CameraRecordingAggregate> cameraMap=new HashMap<>();
		
		 for(CameraRecordingAggregate recordingAggregate : recordingAggregates)
		 {
			 cameraMap.put(recordingAggregate.getDeviceId(), recordingAggregate);
		 }
		 
		 List<CameraSummaryResponse> cameraSummaryResponses=new ArrayList<>();
		 
		 for(Device device : devices)
		 {			 
			CameraLiveState liveState=resolveLiveStatus(device);
			
			CameraRecordingAggregate recordingAggregate=cameraMap.get(device.getId());
			
			CameraRecordingState recordingState=resolveRecordingState(recordingAggregate);
			
			cameraSummaryResponses.add(new CameraSummaryResponse
											(
												device.getId(),
												device.getName(), 
												device.getLocation(), 
												device.getStatus(),
												liveState.getLiveStatus(), 
												liveState.getLatestLiveFrameTime(), 
												recordingState.isRecordingAvailable(),
												recordingState.getRecordingSegmentCount(),
												recordingState.getEarliestRecordingTime(),
												recordingState.getLatestRecordingTime()
											));
		 }
		 
		 return cameraSummaryResponses;
	}
	
	private CameraLiveState resolveLiveStatus(Device device)
	{
		CameraLiveStatus liveStatus;
		Instant latestLiveFrameTime;
		
		if(device.getStatus()==DeviceStatus.OFFLINE
				|| device.getStatus()==DeviceStatus.UNKNOWN)
		{
			liveStatus=CameraLiveStatus.UNAVAILABLE;
			latestLiveFrameTime=null;
		}
		else
		{
			Optional<LiveFrameData> optionalFrame=
					liveFrameService.getLatestFrame(device.getId());

			if(optionalFrame.isPresent())
			{
				LiveFrameData frame=optionalFrame.get();
			
				liveStatus=CameraLiveStatus.LIVE;
				latestLiveFrameTime=frame.getReceivedAt();
			}
			else
			{
				liveStatus=CameraLiveStatus.STALE;
				latestLiveFrameTime=null;
			}
		}
		
		return new CameraLiveState(liveStatus, latestLiveFrameTime);
	}
	
	private CameraRecordingState resolveRecordingState(CameraRecordingTimelineAggregate aggregate)
	{
		if(aggregate==null)
			throw new IllegalArgumentException("Aggregate cannot be null");
		
		return new CameraRecordingState(
				aggregate.getTotalSegmentCount(), 
				aggregate.getEarliestRecordingTime(),
				aggregate.getLatestRecordingTime()
				);
	}
	
	private CameraRecordingState resolveRecordingState(CameraRecordingAggregate aggregate)
	{
		if(aggregate==null)
			return new CameraRecordingState(0, null, null);
		
		return new CameraRecordingState(
				aggregate.getRecordingSegmentCount(), 
				aggregate.getEarliestRecordingTime(),
				aggregate.getLatestRecordingTime()
				);
	}
	
	private List<SecurityEventResponse> toRecentEventResponses(List<SecurityEvent> events)
	{
	    if (events == null)
	        throw new IllegalArgumentException("Events cannot be null");

	    if (events.isEmpty())
	        return List.of();

	    List<UUID> eventIds = events.stream()
	            .map(SecurityEvent::getId)
	            .toList();

	    Map<UUID, Snapshot> snapshotsByEventId = new HashMap<>();
	    for (Snapshot snapshot : snapshotRepository.findByEventIdIn(eventIds))
	    {
	        snapshotsByEventId.put(snapshot.getEvent().getId(), snapshot);
	    }

	    Map<UUID, VideoClip> clipsByEventId = new HashMap<>();
	    for (VideoClip clip : videoClipRepository.findByEventIdIn(eventIds))
	    {
	        clipsByEventId.put(clip.getEvent().getId(), clip);
	    }

	    List<SecurityEventResponse> responses = new ArrayList<>(events.size());
	    for (SecurityEvent event : events)
	    {
	        UUID eventId = event.getId();
	        responses.add(SecurityEventResponse.from(
	                event,
	                snapshotsByEventId.get(eventId),
	                clipsByEventId.get(eventId)
	        ));
	    }

	    return responses;
	}
}