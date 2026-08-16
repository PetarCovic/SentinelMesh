package com.sentinelmesh.clips;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public class VideoClipResource 
{
	private final Resource resource;
	private final MediaType mediaType;
	private final String filename;
	
	public VideoClipResource(
			Resource resource,
			MediaType mediaType,
			String filename
			)
	{
		this.resource=resource;
		this.mediaType=mediaType;
		this.filename=filename;	
	}
	
	public Resource getResource()
	{
		return resource;
	}
	
	public MediaType getMediaType()
	{
		return mediaType;
	}
	
	public String getFilename()
	{
		return filename;
	}
}
