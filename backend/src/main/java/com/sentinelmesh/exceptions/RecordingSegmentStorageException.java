package com.sentinelmesh.exceptions;

public class RecordingSegmentStorageException extends RuntimeException
{
	public RecordingSegmentStorageException(String msg)
	{
		super(msg);
	}
	
	public RecordingSegmentStorageException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}