package com.sentinelmesh.cameras;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cameras")
public class CameraController 
{
	private final CameraService cameraService;
	
	public CameraController(CameraService cameraService)
	{
		if(cameraService==null)
			throw new IllegalArgumentException("CameraService cannot be null");
		
		this.cameraService=cameraService;
	}
	
	@GetMapping("/{deviceId}")
	public CameraDetailsResponse getCameraDetails(@PathVariable UUID deviceId)
	{
		return cameraService.getCameraDetails(deviceId);
	}
	
	@GetMapping("")
	public List<CameraSummaryResponse> getCameraSummaries()
	{
		return cameraService.getCameraSummaries();
	}
}
