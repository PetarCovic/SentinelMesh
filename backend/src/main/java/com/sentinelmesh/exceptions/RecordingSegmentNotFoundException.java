package com.sentinelmesh.exceptions;

import java.util.UUID;

public class RecordingSegmentNotFoundException extends RuntimeException
{
	public RecordingSegmentNotFoundException(UUID id)
	{
		super("Recording Segment not found with id: "+id);
	}
}