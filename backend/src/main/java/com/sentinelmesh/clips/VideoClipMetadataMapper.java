package com.sentinelmesh.clips;

import org.springframework.stereotype.Component;

@Component
public class VideoClipMetadataMapper 
{
	public VideoClipResponse toResponse(VideoClip videoClip)
	{
		if(videoClip==null)
			throw new IllegalArgumentException("VideoClip cannot be null");
		
		return new VideoClipResponse(
				videoClip.getId(), 
				videoClip.getEvent().getId(),
				videoClip.getDevice().getId(),
				videoClip.getContentType(),
				videoClip.getFileSizeBytes(),
				videoClip.getDurationSeconds(),
				videoClip.getWidth(),
				videoClip.getHeight(),
				"/api/clips/"+videoClip.getId()+"/video",
				videoClip.getCreatedAt()
				);
	}
}
