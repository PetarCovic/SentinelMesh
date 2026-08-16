package com.sentinelmesh.recordings;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.sentinelmesh.devices.Device;
import com.sentinelmesh.devices.DeviceRepository;
import com.sentinelmesh.devices.DeviceType;
import com.sentinelmesh.exceptions.DeviceNotFoundException;
import com.sentinelmesh.exceptions.RecordingSegmentNotFoundException;
import com.sentinelmesh.exceptions.RecordingSegmentValidationException;

@Service
public class RecordingSegmentService 
{
	private final RecordingSegmentRepository recordingSegmentRepository;
	private final RecordingSegmentStorageService recordingSegmentStorageService;
	private final RecordingSegmentMetadataMapper recordingSegmentMetadataMapper;
	private final DeviceRepository deviceRepository;
	
	public RecordingSegmentService(
			RecordingSegmentRepository recordingSegmentRepository,
			RecordingSegmentStorageService recordingSegmentStorageService,
			RecordingSegmentMetadataMapper recordingSegmentMetadataMapper,
			DeviceRepository deviceRepository
			)
	{
		this.recordingSegmentRepository=recordingSegmentRepository;
		this.recordingSegmentStorageService=recordingSegmentStorageService;
		this.recordingSegmentMetadataMapper=recordingSegmentMetadataMapper;
		this.deviceRepository=deviceRepository;
	}
	
	@Transactional
	public RecordingSegmentResponse uploadRecordingSegment(
			UUID deviceId,
			UUID segmentId,
			MultipartFile file,
			Instant segmentStartTime,
			Instant segmentEndTime)
	{
		if(deviceId==null)
			throw new IllegalArgumentException("DeviceId cannot be null");
		
		if(file==null)
			throw new IllegalArgumentException("File cannot be null");
		
		if(segmentId==null)
			throw new IllegalArgumentException("SegmentId cannot be null");
		
		if(segmentStartTime==null)
			throw new RecordingSegmentValidationException("SegmentStartTime cannot be null");

		if(segmentEndTime==null)
			throw new RecordingSegmentValidationException("SegmentEndTime cannot be null");

		if(!segmentEndTime.isAfter(segmentStartTime))
			throw new RecordingSegmentValidationException("End time must be after start time");
		
		Optional<RecordingSegment> existingSegment =
		        recordingSegmentRepository.findBySegmentId(segmentId);

		if(existingSegment.isPresent())
		{
		    RecordingSegment existing = existingSegment.get();

		    if(!existing.getDevice().getId().equals(deviceId))
		    {
		        throw new RecordingSegmentValidationException(
		                "SegmentId is already associated with another device"
		        );
		    }

		    return recordingSegmentMetadataMapper.toResponse(existing);
		}
		
		Device device=deviceRepository.findById(deviceId).orElseThrow(
			    () -> new DeviceNotFoundException(deviceId));
		
		StoredRecordingSegmentFile storedRecordingSegment=recordingSegmentStorageService.storeRecordingSegmentFile(
				deviceId, 
				segmentId,
				segmentStartTime,
				segmentEndTime,
				file
				);
		
		String originalFilename=file.getOriginalFilename();
		if(originalFilename==null || originalFilename.isBlank())
			originalFilename=storedRecordingSegment.getStoredFilename();
		
		try
		{
			RecordingSegment recordingSegment=new RecordingSegment(
					segmentId,
					device,
					originalFilename,
					storedRecordingSegment.getStoredFilename(),
					storedRecordingSegment.getContentType(),
					storedRecordingSegment.getFileSizeBytes(),
					storedRecordingSegment.getDurationSeconds(),
					storedRecordingSegment.getWidth(),
					storedRecordingSegment.getHeight(),
					storedRecordingSegment.getStoragePath().toString(),
					segmentStartTime,
					segmentEndTime);
			
			RecordingSegment savedRecordingSegment=recordingSegmentRepository.save(recordingSegment);
			
			return recordingSegmentMetadataMapper.toResponse(savedRecordingSegment);
		}
		catch(RuntimeException ex)
		{
			recordingSegmentStorageService.deleteRecordingSegmentIfSaveFailed(storedRecordingSegment);
			throw ex;
		}
	}
	
	@Transactional(readOnly=true)
	public RecordingSegmentResponse getRecordingSegmentMetadata(UUID segmentId)
	{
		if(segmentId==null)
			throw new IllegalArgumentException("SegmentID cannot be null");
		
		RecordingSegment recordingSegment=recordingSegmentRepository.findById(segmentId).orElseThrow(
				() -> new RecordingSegmentNotFoundException(segmentId));
		
		return recordingSegmentMetadataMapper.toResponse(recordingSegment);
	}
	
	@Transactional(readOnly=true)
	public RecordingSegment getRecordingSegmentEntity(UUID segmentId)
	{
		if(segmentId==null)
		    throw new IllegalArgumentException("SegmentID cannot be null");
		
		return recordingSegmentRepository.findById(segmentId).orElseThrow(
				() -> new RecordingSegmentNotFoundException(segmentId));
	}
	
	public RecordingSegmentResource loadRecordingSegmentResource(UUID segmentId)
	{
		if(segmentId==null)
			throw new IllegalArgumentException("SegmentId cannot be null");
		
		RecordingSegment recordingSegment=recordingSegmentRepository.findById(segmentId).orElseThrow(
				() -> new RecordingSegmentNotFoundException(segmentId));

		Resource resource=recordingSegmentStorageService.loadRecordingSegmentFile(recordingSegment);
		MediaType mediaType=MediaType.parseMediaType(recordingSegment.getContentType());

		return new RecordingSegmentResource(
				resource,
				mediaType,
				recordingSegment.getStoredFilename(),
				recordingSegment.getFileSizeBytes()
				);
	}
	
	@Transactional(readOnly=true)
	public List<RecordingSegmentResponse> getRecentSegmentsForDevice(UUID deviceId)
	{
		if(deviceId==null)
			throw new IllegalArgumentException("DeviceId cannot be null");
		
		if(!deviceRepository.existsById(deviceId))
			throw new IllegalArgumentException("Device does not exist");
		
		Device device=deviceRepository.findById(deviceId).orElseThrow(
				() -> new DeviceNotFoundException(deviceId));
		
		if(device.getType()!=DeviceType.CAMERA)
			throw new IllegalArgumentException("Device does not have camera");
	
		List<RecordingSegment> recordingSegments=recordingSegmentRepository.findTop20ByDeviceIdOrderBySegmentStartTimeDesc(deviceId);
		
		return recordingSegmentMetadataMapper.toResponse(recordingSegments);
	}
	
	@Transactional(readOnly=true)
	public List<RecordingSegmentResponse> getSegmentsForDeviceBetween(UUID deviceId, Instant start, Instant end)
	{
		if(deviceId==null)
			throw new IllegalArgumentException("DeviceId cannot be null");
	
		if(!deviceRepository.existsById(deviceId))
			throw new IllegalArgumentException("Device does not exist");
		
		if(start==null)
			throw new RecordingSegmentValidationException("Start cannot be null");

		if(end==null)
			throw new RecordingSegmentValidationException("End cannot be null");

		if(!end.isAfter(start))
			throw new RecordingSegmentValidationException("End time must be after start time");
		
		List<RecordingSegment> recordingSegments =
		        recordingSegmentRepository.findOverlappingSegments(
		                deviceId,
		                start,
		                end
		        );
		
		return recordingSegmentMetadataMapper.toResponse(recordingSegments);
	}
}
