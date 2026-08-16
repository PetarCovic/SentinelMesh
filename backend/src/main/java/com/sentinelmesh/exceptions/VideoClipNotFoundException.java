package com.sentinelmesh.exceptions;

import java.util.UUID;

public class VideoClipNotFoundException extends RuntimeException
{
	public VideoClipNotFoundException(UUID id)
	{
		super("VideoClip not found with id: "+id);
	}
}