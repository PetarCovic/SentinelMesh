package com.sentinelmesh.devices;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sentinelmesh.devices.requests.CreateDeviceRequest;
import com.sentinelmesh.devices.requests.UpdateDeviceRequest;
import com.sentinelmesh.exceptions.DeviceNotFoundException;

@Service
public class DeviceService 
{
	private final DeviceRepository deviceRepository;
	
	public DeviceService(DeviceRepository deviceRepository)
	{
		this.deviceRepository=deviceRepository;
	}
	
	@Transactional
	public DeviceResponse createDevice(CreateDeviceRequest request)
	{
		Device device=new Device(request.getName(), 
				request.getType(), 
				request.getLocation());
		
		Device savedDevice=deviceRepository.save(device);
		
		return DeviceResponse.from(savedDevice);
	}
	
	@Transactional(readOnly=true)
	public List<DeviceResponse> getAllDevices()
	{
		return deviceRepository
				.findAll()
				.stream()
				.map(DeviceResponse::from)
				.toList();
	}
	
	@Transactional(readOnly=true)
	public DeviceResponse getDeviceById(UUID id)
	{
		Device device=deviceRepository.findById(id).orElseThrow(() 
				-> new DeviceNotFoundException(id));
		
		return DeviceResponse.from(device);
	}
	
	@Transactional
	public DeviceResponse updateDevice(UUID id, UpdateDeviceRequest request)
	{
		Device device=deviceRepository.findById(id).orElseThrow(()
				-> new DeviceNotFoundException(id));
		
		if(request.getName()!=null)
			device.setName(request.getName());
		
		if(request.getType()!=null)
			device.setType(request.getType());
		
		if(request.getLocation()!=null)
			device.setLocation(request.getLocation());
		
		Device savedDevice=deviceRepository.save(device);
		
		return DeviceResponse.from(savedDevice);
	}
	
	@Transactional
	public void deleteDevice(UUID id)
	{
		Device device=deviceRepository.findById(id).orElseThrow(()
				-> new DeviceNotFoundException(id));
		
		deviceRepository.delete(device);
	}
}
