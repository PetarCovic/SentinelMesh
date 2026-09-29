package com.sentinelmesh.recordings;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.sentinelmesh.common.PageResponse;
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
		
		RecordingSegment recordingSegment=recordingSegmentRepository.findBySegmentId(segmentId).orElseThrow(
				() -> new RecordingSegmentNotFoundException(segmentId));
		
		return recordingSegmentMetadataMapper.toResponse(recordingSegment);
	}
	
	public RecordingSegmentResource loadRecordingSegmentResource(UUID segmentId)
	{
		if(segmentId==null)
			throw new IllegalArgumentException("SegmentId cannot be null");
		
		RecordingSegment recordingSegment=recordingSegmentRepository.findBySegmentId(segmentId).orElseThrow(
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
	public List<RecordingSegmentResponse> getSegmentsForDeviceBetween(
			UUID deviceId, 
			Instant start,
			Instant end,
			long maxDuration)
	{
		if(deviceId==null)
			throw new IllegalArgumentException("DeviceId cannot be null");
		
		if(start==null)
			throw new RecordingSegmentValidationException("Start cannot be null");

		if(end==null)
			throw new RecordingSegmentValidationException("End cannot be null");

		if(!end.isAfter(start))
			throw new RecordingSegmentValidationException("End time must be after start time");
		
		if(maxDuration<=0)
			throw new IllegalArgumentException("MaxDuration must be positive");
		
		if(Duration.between(start, end).compareTo(Duration.ofDays(maxDuration))>0)
			throw new RecordingSegmentValidationException("Duration between start and end must be "
					+ "less than or equal to "+maxDuration+" days");
		
		Device device=deviceRepository.findById(deviceId).orElseThrow(
			    () -> new DeviceNotFoundException(deviceId));
		
		if(device.getType()!=DeviceType.CAMERA)
			throw new IllegalArgumentException("Device must be a CAMERA device");
		
		List<RecordingSegment> recordingSegments =
		        recordingSegmentRepository.findOverlappingSegments(
		                deviceId,
		                start,
		                end
		        );
		
		return recordingSegmentMetadataMapper.toResponse(recordingSegments);
	}
	
	@Transactional(readOnly=true)
	public List<RecordingSegmentResponse> getSegmentsForDeviceBetween(UUID deviceId, Instant start, Instant end)
	{
		return getSegmentsForDeviceBetween(deviceId, start, end, 7);
	}
	
	@Transactional(readOnly = true)
	public PageResponse<RecordingSegmentResponse> getSegmentPageForDeviceBetween(
	        UUID deviceId,
	        Instant start,
	        Instant end,
	        int page,
	        int size
	) 
	{
	    if (deviceId == null)
	        throw new IllegalArgumentException("DeviceId cannot be null");

	    if (start == null)
	        throw new RecordingSegmentValidationException("Start cannot be null");

	    if (end == null)
	        throw new RecordingSegmentValidationException("End cannot be null");

	    if (!end.isAfter(start))
	        throw new RecordingSegmentValidationException(
	                "End time must be after start time");

	    if (Duration.between(start, end).compareTo(Duration.ofDays(7)) > 0)
	        throw new RecordingSegmentValidationException(
	                "Requested range must be at most 7 days");

	    if (page < 0)
	        throw new RecordingSegmentValidationException(
	                "Page cannot be negative");

	    if (size < 1 || size > 100)
	        throw new RecordingSegmentValidationException("Size must be between 1 and 100");

	    if (!compareDeviceType(deviceId, DeviceType.CAMERA))
	        throw new IllegalArgumentException("Device must be a CAMERA device");

	    Page<RecordingSegment> segmentPage =
	            recordingSegmentRepository.findOverlappingSegmentsPage(
	                    deviceId,
	                    start,
	                    end,
	                    PageRequest.of(page, size)
	            );

	    return PageResponse.from(
	            segmentPage.map(recordingSegmentMetadataMapper::toResponse)
	    );
	}
	
	public List<CameraRecordingGapResponse> calculateGaps(
            Instant requestedStart,
            Instant requestedEnd,
            List<RecordingSegment> segments
    ) {
        if (requestedStart == null || requestedEnd == null || segments == null)
            throw new IllegalArgumentException("Timeline inputs cannot be null");

        if (!requestedEnd.isAfter(requestedStart))
            throw new IllegalArgumentException("Requested end must be after start");

        List<RecordingSegment> orderedSegments = new ArrayList<>(segments);
        orderedSegments.sort(
                Comparator.comparing(RecordingSegment::getSegmentStartTime)
                        .thenComparing(RecordingSegment::getSegmentEndTime)
        );

        List<CameraRecordingGapResponse> gaps = new ArrayList<>();
        Instant coveredUntil = requestedStart;

        for (RecordingSegment segment : orderedSegments) {
            Instant segmentStart = segment.getSegmentStartTime();
            Instant segmentEnd = segment.getSegmentEndTime();

            Instant clippedStart = segmentStart.isBefore(requestedStart)
                    ? requestedStart : segmentStart;
            Instant clippedEnd = segmentEnd.isAfter(requestedEnd)
                    ? requestedEnd : segmentEnd;

            if (!clippedEnd.isAfter(clippedStart))
                continue;

            if (clippedStart.isAfter(coveredUntil))
                gaps.add(createGap(coveredUntil, clippedStart));

            if (clippedEnd.isAfter(coveredUntil))
                coveredUntil = clippedEnd;

            if (!coveredUntil.isBefore(requestedEnd))
                break;
        }

        if (coveredUntil.isBefore(requestedEnd))
            gaps.add(createGap(coveredUntil, requestedEnd));

        return List.copyOf(gaps);
    }
	
	@Transactional(readOnly=true)
	public CameraRecordingTimelineResponse getCameraRecordingTimeline(
			UUID deviceId,
			Instant start,
			Instant end,
			int page,
			int size
			)
	{
		PageResponse<RecordingSegmentResponse> segments=
	    		getSegmentPageForDeviceBetween(
	    				deviceId,
	    				start,
	    				end,
	    				page,
	    				size
	    				);

	    CameraRecordingTimelineAggregate recordingTimelineAggregate=
	    		recordingSegmentRepository
	    			.getCameraRecordingTimelineAggregate(deviceId);
	    
	    List<RecordingSegment> overlappingSegments=
	    		recordingSegmentRepository.findOverlappingSegments(
	    				deviceId, 
	    				start, 
	    				end
	    				);
	    
	    List<CameraRecordingGapResponse> gaps=calculateGaps(
	    		start, 
	    		end,
	    		overlappingSegments
	    		);
	    
	    return new CameraRecordingTimelineResponse(
	    		deviceId,
				start,
				end,
				recordingTimelineAggregate.getEarliestRecordingTime(),
				recordingTimelineAggregate.getLatestRecordingTime(),
				recordingTimelineAggregate.getTotalSegmentCount(),
				recordingTimelineAggregate.getTotalRecordedDurationSeconds(),
				segments,
				gaps
	    		);
	}

    private CameraRecordingGapResponse createGap(Instant start, Instant end) {
        return new CameraRecordingGapResponse(
                start,
                end,
                Duration.between(start, end).getSeconds()
        );
    }
	
	private boolean compareDeviceType(UUID deviceId, DeviceType toType) 
	{
	    if (deviceId == null) {
	        throw new IllegalArgumentException("DeviceId cannot be null");
	    }

	    DeviceType fromType = deviceRepository.findTypeById(deviceId)
	            .orElseThrow(() -> new DeviceNotFoundException(deviceId));

	    if (fromType != toType) 
	        return false;
	    
	    return true;
	}
}