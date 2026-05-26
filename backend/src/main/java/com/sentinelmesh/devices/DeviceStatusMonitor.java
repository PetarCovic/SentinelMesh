package com.sentinelmesh.devices;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceStatusMonitor 
{
	private static final Duration OFFLINE_THRESHOLD=Duration.ofSeconds(30);
	
	private final DeviceRepository deviceRepository;
	
	public DeviceStatusMonitor(DeviceRepository deviceRepository)
	{
		this.deviceRepository=deviceRepository;
	}
	
	@Scheduled(fixedRate=10_000)
	@Transactional
	public void markStaleDevicesOffline()
	{
		Instant cutoff=Instant.now().minus(OFFLINE_THRESHOLD);
		
		List<Device> onlineDevices=deviceRepository.findByStatus(DeviceStatus.ONLINE);
		
		for(Device device : onlineDevices)
		{
			Instant lastSeenAt=device.getLastSeenAt();
			
			if(lastSeenAt==null || lastSeenAt.isBefore(cutoff))
				device.setStatus(DeviceStatus.OFFLINE);
		}
	}
}
