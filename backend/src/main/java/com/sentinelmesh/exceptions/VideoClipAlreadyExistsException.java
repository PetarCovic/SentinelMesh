package com.sentinelmesh.exceptions;

import java.util.UUID;

public class VideoClipAlreadyExistsException extends RuntimeException
{
	public VideoClipAlreadyExistsException(UUID id)
	{
		super("VideoClip already exists with id: "+id);
	}
}
