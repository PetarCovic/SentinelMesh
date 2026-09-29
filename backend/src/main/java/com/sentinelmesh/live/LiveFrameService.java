package com.sentinelmesh.live;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.sentinelmesh.devices.Device;
import com.sentinelmesh.devices.DeviceRepository;
import com.sentinelmesh.devices.DeviceType;
import com.sentinelmesh.exceptions.DeviceNotFoundException;

@Service
public class LiveFrameService 
{
	private final LiveFrameStore liveFrameStore;
	private final DeviceRepository deviceRepository;
	
	private final long LIVE_FRAME_STALE_THRESHOLD_SECONDS=10;

	public LiveFrameService(LiveFrameStore liveFrameStore, DeviceRepository deviceRepository)
	{
		if(liveFrameStore==null)
			throw new IllegalArgumentException("LiveFrameStore cannot be null");
		
		if(deviceRepository==null)
			throw new IllegalArgumentException("DeviceRepository cannot be null");
		
		this.liveFrameStore=liveFrameStore;
		this.deviceRepository = deviceRepository;
	}
	
	public void storeFrame(UUID deviceId, MultipartFile file)
	{
		if(deviceId==null)
			throw new IllegalArgumentException("DeviceId cannot be null");
		
		Device device=deviceRepository.findById(deviceId).orElseThrow(
			    () -> new DeviceNotFoundException(deviceId));
		
		if(device.getType()!=DeviceType.CAMERA)
			throw new IllegalArgumentException("DeviceType must be CAMERA");
		
		if(file==null || file.isEmpty())
			throw new IllegalArgumentException("File cannot be null or empty");
		
		String contentType=file.getContentType();
		
		if(contentType==null)
			throw new IllegalArgumentException("ContentType cannot be null");
		
		if(!contentType.equals("image/jpeg"))
			throw new IllegalArgumentException("File must be a jpeg");
		
		if(file.getSize()>2000000)
			throw new IllegalArgumentException("File size must be less than or equal to 2MB");
		
		byte[] jpegBytes;
		
		try {
			jpegBytes = file.getBytes();
			
			if(jpegBytes==null || jpegBytes.length==0)
				throw new IllegalArgumentException("JpegBytes cannot be null or of length 0");
			
			if(jpegBytes.length < 4)
				throw new IllegalArgumentException("JPEG file is too small to be valid");

			boolean validStart =
					(jpegBytes[0] & 0xFF) == 0xFF
					&& (jpegBytes[1] & 0xFF) == 0xD8;

			boolean validEnd =
					(jpegBytes[jpegBytes.length - 2] & 0xFF) == 0xFF
					&& (jpegBytes[jpegBytes.length - 1] & 0xFF) == 0xD9;

			if(!validStart || !validEnd)
				throw new IllegalArgumentException("Uploaded file is not a valid JPEG");
			
			LiveFrameData liveFrameData=new LiveFrameData(deviceId, jpegBytes, Instant.now());
			liveFrameStore.put(liveFrameData);
		} catch (IOException e) 
		{
			throw new IllegalStateException("Failed to read live frame upload", e);
		}
	}
	
	public Optional<LiveFrameData> getLatestFrame(UUID deviceId)
	{
		if(deviceId==null)
			throw new IllegalArgumentException("DeviceId cannot be null");
		
		Optional<LiveFrameData> optionalFrame=liveFrameStore.get(deviceId);
		
		if(optionalFrame.isEmpty())
			return Optional.empty();
		
		LiveFrameData frame = optionalFrame.get();
		
		Instant staleCutoff =
				Instant.now().minusSeconds(LIVE_FRAME_STALE_THRESHOLD_SECONDS);
		
		if(!frame.getReceivedAt().isBefore(staleCutoff))
			return Optional.of(frame);
		
			liveFrameStore.remove(deviceId, frame);
		
		return Optional.empty();
	}
	
	public void removeFrame(UUID deviceId)
	{
		if(deviceId==null)
			throw new IllegalArgumentException("DeviceId cannot be null");
		
		liveFrameStore.remove(deviceId);
	}
	
	public void validateCamera(UUID deviceId)
	{
		if(deviceId==null)
			throw new IllegalArgumentException("DeviceId cannot be null");
		
		Device device=deviceRepository.findById(deviceId).orElseThrow(
			    () -> new DeviceNotFoundException(deviceId));
		
		if(device.getType()!=DeviceType.CAMERA)
			throw new IllegalArgumentException("DeviceType must be CAMERA");
	}
}