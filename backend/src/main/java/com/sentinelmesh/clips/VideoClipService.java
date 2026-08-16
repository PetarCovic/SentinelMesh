package com.sentinelmesh.clips;

import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.sentinelmesh.devices.Device;
import com.sentinelmesh.devices.DeviceRepository;
import com.sentinelmesh.events.SecurityEvent;
import com.sentinelmesh.events.SecurityEventRepository;
import com.sentinelmesh.exceptions.DeviceNotFoundException;
import com.sentinelmesh.exceptions.SecurityEventNotFoundException;
import com.sentinelmesh.exceptions.VideoClipAlreadyExistsException;
import com.sentinelmesh.exceptions.VideoClipNotFoundException;
import com.sentinelmesh.exceptions.VideoClipOwnershipException;

@Service
public class VideoClipService 
{
	private final VideoClipRepository videoClipRepository;
	private final VideoClipStorageService videoClipStorageService;
	private final VideoClipMetadataMapper videoClipMetadataMapper;
	private final DeviceRepository deviceRepository;
	private final SecurityEventRepository securityEventRepository;
	
	public VideoClipService(
			VideoClipRepository videoClipRepository,
			VideoClipStorageService videoClipStorageService,
			VideoClipMetadataMapper videoClipMetadataMapper,
			DeviceRepository deviceRepository,
			SecurityEventRepository securityEventRepository
			)
	{
		this.videoClipRepository=videoClipRepository;
		this.videoClipStorageService=videoClipStorageService;
		this.videoClipMetadataMapper=videoClipMetadataMapper;
		this.deviceRepository=deviceRepository;
		this.securityEventRepository=securityEventRepository;
	}
	
	@Transactional
	public VideoClipResponse uploadVideoClip(UUID deviceId, UUID eventId, MultipartFile file)
	{
		if(deviceId==null)
			throw new IllegalArgumentException("DeviceId cannot be null");
		
		if(eventId==null)
			throw new IllegalArgumentException("EventId cannot be null");
		
		if(file==null)
			throw new IllegalArgumentException("File cannot be null");
		
		Device device=deviceRepository.findById(deviceId).orElseThrow(
			    () -> new DeviceNotFoundException(deviceId));
		
		SecurityEvent event=securityEventRepository.findById(eventId).orElseThrow(
				() -> new SecurityEventNotFoundException(eventId));
		
		if(!event.getDevice().getId().equals(device.getId()))
			throw new VideoClipOwnershipException("Event device must be the same as device");
		
		if(videoClipRepository.existsByEventId(eventId))
		    throw new VideoClipAlreadyExistsException(eventId);
		
		StoredVideoClipFile storedVideoClip=videoClipStorageService.storeVideoClipFile(
				deviceId, 
				eventId, 
				UUID.randomUUID(), 
				file
				);
		
		String originalFilename=file.getOriginalFilename();
		if(originalFilename==null || originalFilename.isBlank())
			originalFilename=storedVideoClip.getStoredFilename();
		
		try
		{
			VideoClip videoClip=new VideoClip(
					event,
					device,
					originalFilename,
					storedVideoClip.getStoredFilename(),
					storedVideoClip.getContentType(),
					storedVideoClip.getFileSizeBytes(),
					storedVideoClip.getDurationSeconds(),
					storedVideoClip.getWidth(),
					storedVideoClip.getHeight(),
					storedVideoClip.getStoragePath().toString());
			
			VideoClip savedVideoClip=videoClipRepository.save(videoClip);
			
			return videoClipMetadataMapper.toResponse(savedVideoClip);
		}
		catch(RuntimeException ex)
		{
			videoClipStorageService.deleteVideoClipIfSaveFailed(storedVideoClip);
			throw ex;
		}
	}
	
	@Transactional(readOnly=true)
	public VideoClipResponse getVideoClipMetadata(UUID clipId)
	{
		if(clipId==null)
			throw new IllegalArgumentException("ClipID cannot be null");
		
		VideoClip videoClip=videoClipRepository.findById(clipId).orElseThrow(
				() -> new VideoClipNotFoundException(clipId));
		
		return videoClipMetadataMapper.toResponse(videoClip);
	}
	
	public Resource loadVideoClipVideo(UUID clipId)
	{
		if(clipId==null)
		    throw new IllegalArgumentException("ClipID cannot be null");
		
		VideoClip videoClip=videoClipRepository.findById(clipId).orElseThrow(
				() -> new VideoClipNotFoundException(clipId));
		
		return videoClipStorageService.loadVideoClipFile(videoClip);
	}
	
	@Transactional(readOnly=true)
	public VideoClip getVideoClipEntity(UUID clipId)
	{
		if(clipId==null)
		    throw new IllegalArgumentException("ClipID cannot be null");
		
		return videoClipRepository.findById(clipId).orElseThrow(
				() -> new VideoClipNotFoundException(clipId));
	}
	
	public VideoClipResource loadVideoClipResource(UUID clipId)
	{
		VideoClip videoClip=videoClipRepository.findById(clipId).orElseThrow(
				() -> new VideoClipNotFoundException(clipId));

		Resource resource=videoClipStorageService.loadVideoClipFile(videoClip);
		MediaType mediaType=MediaType.parseMediaType(videoClip.getContentType());

		return new VideoClipResource(
				resource,
				mediaType,
				videoClip.getStoredFilename()
				);
	}
}
