package com.sentinelmesh.recordings;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class RecordingSegmentMetadataMapper 
{
	public RecordingSegmentResponse toResponse(RecordingSegment recordingSegment)
	{
		if(recordingSegment==null)
			throw new IllegalArgumentException("RecordingSegment cannot be null");
		
		return new RecordingSegmentResponse(
				recordingSegment.getId(), 
				recordingSegment.getDevice().getId(),
				recordingSegment.getSegmentId(),
				recordingSegment.getContentType(),
				recordingSegment.getFileSizeBytes(),
				recordingSegment.getDurationSeconds(),
				recordingSegment.getWidth(),
				recordingSegment.getHeight(),
				"/api/recordings/segments/"+recordingSegment.getId()+"/video",
				recordingSegment.getSegmentStartTime(),
				recordingSegment.getSegmentEndTime(),
				recordingSegment.getCreatedAt()
				);
	}
	
	public List<RecordingSegmentResponse> toResponse(List<RecordingSegment> recordingSegments)
	{
		List<RecordingSegmentResponse> recordingSegmentResponseList=new ArrayList<>();
		
		for(RecordingSegment segment : recordingSegments)
		{
			recordingSegmentResponseList.add(toResponse(segment));
		}
		
		return recordingSegmentResponseList;
	}
}
