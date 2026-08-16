package com.sentinelmesh.exceptions;

public class VideoClipStorageException extends RuntimeException
{
	public VideoClipStorageException(String msg)
	{
		super(msg);
	}
	
	public VideoClipStorageException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}