package com.sentinelmesh.devices;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sentinelmesh.devices.requests.CreateDeviceRequest;
import com.sentinelmesh.devices.requests.UpdateDeviceRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/devices")
public class DeviceController 
{
	private final DeviceService deviceService;
	
	public DeviceController(DeviceService deviceService)
	{
		this.deviceService=deviceService;
	}
	
	@PostMapping
	public ResponseEntity<CreateDeviceResponse> createDevice(
			@Valid @RequestBody CreateDeviceRequest request)
	{
		CreateDeviceResponse response=deviceService.createDevice(request);
		
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
	
	@PatchMapping("/{id}")
	public ResponseEntity<DeviceResponse> updateDevice(
			@PathVariable UUID id, @Valid @RequestBody UpdateDeviceRequest request)
	{
		DeviceResponse response=deviceService.updateDevice(id, request);
		
		return ResponseEntity.ok(response);
	}
	
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteDevice(@PathVariable UUID id)
	{
		deviceService.deleteDevice(id);
		
		return ResponseEntity.noContent().build();
	}
	
	@GetMapping
	public ResponseEntity<List<DeviceResponse>> getAllDevices()
	{
		return ResponseEntity.ok(deviceService.getAllDevices());
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<DeviceResponse> getDeviceById(@PathVariable UUID id)
	{
		return ResponseEntity.ok(deviceService.getDeviceById(id));
	}
}
