package com.sentinelmesh.edge.client;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class RecordingSegmentUploadClient 
{
	private final SentinelMeshApiClient apiClient;
	
	public RecordingSegmentUploadClient(SentinelMeshApiClient apiClient)
	{
		if(apiClient==null)
			throw new IllegalArgumentException("SentinelMeshApiClient cannot be null");
		
		this.apiClient=apiClient;
	}
	
	public void uploadRecordingSegment(
			UUID deviceId, 
			UUID segmentId,
			String apiKey, 
			byte[] videoBytes,
			Instant segmentStartTime,
			Instant segmentEndTime)
	{
		if(deviceId==null)
			throw new IllegalArgumentException("DeviceId cannot be null");
		
		if(segmentId==null)
			throw new IllegalArgumentException("SegmentId cannot be null");
		
		if(apiKey==null || apiKey.isBlank())
			throw new IllegalArgumentException("ApiKey cannot be null or blank");
		
		if(videoBytes==null || videoBytes.length==0)
			throw new IllegalArgumentException("VideoBytes cannot be null or empty");
		
		if(segmentStartTime==null)
			throw new IllegalArgumentException("SegmentStartTime cannot be null");
		
		if(segmentEndTime==null)
			throw new IllegalArgumentException("SegmentEndTime cannot be null");
		
		if(!segmentEndTime.isAfter(segmentStartTime))
			throw new IllegalArgumentException("SegmentEndTime must be after SegmentStartTime");
		
		String path="/api/devices/"+deviceId+"/recordings/segments";
		String fieldName="file";
		String filename="recording-segment.mp4";
		String contentType="video/mp4";
		
		apiClient.postMultipartFile(
				path, 
				apiKey, 
				fieldName, 
				filename, 
				contentType, 
				videoBytes, 
				Map.of(
						"segmentId", segmentId.toString(),
					    "segmentStartTime", segmentStartTime.toString(),
					    "segmentEndTime", segmentEndTime.toString()
					));
	}
}
