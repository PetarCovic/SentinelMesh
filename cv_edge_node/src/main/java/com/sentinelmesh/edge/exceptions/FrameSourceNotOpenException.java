package com.sentinelmesh.edge.exceptions;

public class FrameSourceNotOpenException extends RuntimeException
{
	public FrameSourceNotOpenException(String sourceName)
	{
		super("VideoCapture not open: "+sourceName);
	}
}
