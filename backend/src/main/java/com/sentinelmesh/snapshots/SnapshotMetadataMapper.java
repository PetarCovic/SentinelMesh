package com.sentinelmesh.snapshots;

import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class SnapshotMetadataMapper 
{
	public SnapshotResponse toResponse(Snapshot snapshot)
	{
		if(snapshot==null)
			throw new IllegalArgumentException("Snapshot cannot be null");
		
		return new SnapshotResponse(
				snapshot.getId(),
				snapshot.getEvent().getId(),
				snapshot.getDevice().getId(),
				snapshot.getContentType(),
				snapshot.getFileSizeBytes(),
				snapshot.getWidth(),
				snapshot.getHeight(),
				buildImageUrl(snapshot.getId()),
				snapshot.getCreatedAt()
				);
	}
	
	public String buildImageUrl(UUID snapshotId)
	{
		if(snapshotId==null)
			throw new IllegalArgumentException("SnapshotId cannot be null");
		
		return "/api/snapshots/"+snapshotId+"/image";
	}
}