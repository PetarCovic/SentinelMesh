package com.sentinelmesh.edge.exceptions;

public class RecordingSpoolException extends RuntimeException
{
	public RecordingSpoolException(String msg)
	{
		super(msg);
	}
	
	public RecordingSpoolException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}
