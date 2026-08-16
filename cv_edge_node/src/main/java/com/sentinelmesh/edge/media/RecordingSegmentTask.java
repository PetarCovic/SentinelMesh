package com.sentinelmesh.edge.media;

import java.util.UUID;

import com.sentinelmesh.edge.client.RecordingSegmentUploadClient;

@Deprecated
public class RecordingSegmentTask implements Runnable, AutoCloseable
{
	private final CompletedRecordingSegment segment;
	private final VideoClipEncoder videoClipEncoder;
	private final RecordingSegmentUploadClient uploadClient;
	private final UUID deviceId;
	private final String apiKey;
	
	public RecordingSegmentTask(
			CompletedRecordingSegment segment,
			VideoClipEncoder videoClipEncoder,
			RecordingSegmentUploadClient uploadClient,
			UUID deviceId,
			String apiKey
			)
	{
		if(segment==null)
			throw new IllegalArgumentException("Segment cannot be null");
			
		if(videoClipEncoder==null)
			throw new IllegalArgumentException("VideoClipEncoder cannot be null");
				
		if(uploadClient==null)
			throw new IllegalArgumentException("UploadClient cannot be null");
					
		if(deviceId==null)
			throw new IllegalArgumentException("DeviceId cannot be null");
						
		if(apiKey==null || apiKey.isBlank())
			throw new IllegalArgumentException("ApiKey cannot be null or blank");
							
		
		this.segment=segment;
		this.videoClipEncoder=videoClipEncoder;
		this.uploadClient=uploadClient;
		this.deviceId=deviceId;
		this.apiKey=apiKey;
	}
	
	@Override
	public void run()
	{
	    try
	    {
	        byte[] encoded =
	                videoClipEncoder.encodeMp4(segment.getFrames(), segment.getEffectiveFPS());

	        uploadClient.uploadRecordingSegment(
	                deviceId,
	                segment.getSegmentId(),
	                apiKey,
	                encoded,
	                segment.getSegmentStartTime(),
	                segment.getSegmentEndTime()
	        );
	    }
	    catch(Exception ex)
	    {
	        System.out.println(
	                "Continuous recording segment failed: "
	                + ex.getMessage()
	        );
	    }
	    finally
	    {
	        close();
	    }
	}
	
	@Override
	public void close()
	{
	    segment.close();
	}
}
