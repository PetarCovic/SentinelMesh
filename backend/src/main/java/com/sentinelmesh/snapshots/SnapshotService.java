package com.sentinelmesh.snapshots;

import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.sentinelmesh.devices.Device;
import com.sentinelmesh.devices.DeviceAuthenticationService;
import com.sentinelmesh.events.SecurityEvent;
import com.sentinelmesh.events.SecurityEventRepository;
import com.sentinelmesh.exceptions.SecurityEventNotFoundException;
import com.sentinelmesh.exceptions.SnapshotAlreadyExistsException;
import com.sentinelmesh.exceptions.SnapshotNotFoundException;
import com.sentinelmesh.exceptions.SnapshotOwnershipException;

@Service
public class SnapshotService 
{
	private final SnapshotRepository snapshotRepository;
	private final SnapshotStorageService snapshotStorageService;
	private final SnapshotMetadataMapper snapshotMetadataMapper;
	private final DeviceAuthenticationService deviceAuthenticationService;
	private final SecurityEventRepository securityEventRepository;
	
	public SnapshotService(
			SnapshotRepository snapshotRepository,
			SnapshotStorageService snapshotStorageService,
			SnapshotMetadataMapper snapshotMetadataMapper,
			DeviceAuthenticationService deviceAuthenticationService,
			SecurityEventRepository securityEventRepository
			)
	{
		this.snapshotRepository=snapshotRepository;
		this.snapshotStorageService=snapshotStorageService;
		this.snapshotMetadataMapper=snapshotMetadataMapper;
		this.deviceAuthenticationService=deviceAuthenticationService;
		this.securityEventRepository=securityEventRepository;
	}
	
	public SnapshotResponse uploadSnapshotForEvent(
			UUID deviceId, 
			UUID eventId, 
			String rawApiKey, 
			MultipartFile file
			)
	{
		Device device=deviceAuthenticationService.authenticate(deviceId, rawApiKey);
		
		SecurityEvent event=securityEventRepository.findById(eventId).orElseThrow(
				() -> new SecurityEventNotFoundException(eventId));
		
		if(!event.getDevice().getId().equals(device.getId()))
			throw new SnapshotOwnershipException("Event device must be the same as device");
		
		if(snapshotRepository.existsByEventId(eventId))
			throw new SnapshotAlreadyExistsException(eventId);
			
		StoredSnapshotFile storedSnapshot=snapshotStorageService.storeSnapshotFile(
				deviceId, 
				eventId, 
				eventId, 
				file
				);
		
		String originalFilename=file.getOriginalFilename();
		if(originalFilename==null || originalFilename.isBlank())
			originalFilename=storedSnapshot.getStoredFilename();
		
		try
		{
			Snapshot snapshot=new Snapshot(
					event,
					device,
					originalFilename,
					storedSnapshot.getStoredFilename(),
					storedSnapshot.getContentType(),
					storedSnapshot.getFileSizeBytes(),
					storedSnapshot.getWidth(),
					storedSnapshot.getHeight(),
					storedSnapshot.getStoragePath().toString());
			
			Snapshot savedSnapshot=snapshotRepository.save(snapshot);
			
			return snapshotMetadataMapper.toResponse(savedSnapshot);
		}
		catch(RuntimeException ex)
		{
			snapshotStorageService.deleteSnapshotIfSaveFailed(storedSnapshot);
			throw ex;
		}
	}
	
	public SnapshotResponse getSnapshot(UUID snapshotId)
	{
		Snapshot snapshot=snapshotRepository.findById(snapshotId).orElseThrow(
				() -> new SnapshotNotFoundException(snapshotId));
		
		return snapshotMetadataMapper.toResponse(snapshot);
	}
	
	public SnapshotResponse getSnapshotForEvent(UUID eventId)
	{
		Snapshot snapshot=snapshotRepository.findByEventId(eventId).orElseThrow(
				() -> new SnapshotNotFoundException(eventId));
		
		return snapshotMetadataMapper.toResponse(snapshot);
	}
	
	public Resource loadSnapshotImage(UUID snapshotId)
	{
		Snapshot snapshot=snapshotRepository.findById(snapshotId).orElseThrow(
				() -> new SnapshotNotFoundException(snapshotId));
		
		return snapshotStorageService.loadSnapshotFile(snapshot);
	}
	
	public SnapshotImageResource loadSnapshotImageResource(UUID snapshotId)
	{
		Snapshot snapshot=snapshotRepository.findById(snapshotId).orElseThrow(
				() -> new SnapshotNotFoundException(snapshotId));

		Resource resource=snapshotStorageService.loadSnapshotFile(snapshot);
		MediaType mediaType=MediaType.parseMediaType(snapshot.getContentType());

		return new SnapshotImageResource(
				resource,
				mediaType,
				snapshot.getStoredFilename()
				);
	}
}