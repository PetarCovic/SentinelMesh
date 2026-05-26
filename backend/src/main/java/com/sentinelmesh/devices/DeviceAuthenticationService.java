package com.sentinelmesh.devices;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sentinelmesh.exceptions.DeviceNotFoundException;
import com.sentinelmesh.exceptions.InvalidDeviceApiKeyException;

@Service
public class DeviceAuthenticationService 
{
	private final DeviceRepository deviceRepository;
	private final ApiKeyHashService apiKeyHashService;
	
	public DeviceAuthenticationService(
			DeviceRepository deviceRepository, 
			ApiKeyHashService apiKeyHashService
			)
	{
		this.deviceRepository=deviceRepository;
		this.apiKeyHashService=apiKeyHashService;
	}
	
	@Transactional(readOnly=true)
	public Device authenticate(UUID id, String rawApiKey)
	{
		Device device=deviceRepository.findById(id).orElseThrow(()
				-> new DeviceNotFoundException(id));
		
		if(device.getApiKeyHash()==null 
				|| !apiKeyHashService.matches(rawApiKey, device.getApiKeyHash()))
			throw new InvalidDeviceApiKeyException(id);
		
		return device;
	}
}
