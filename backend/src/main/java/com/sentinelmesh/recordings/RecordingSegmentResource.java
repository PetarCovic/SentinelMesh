package com.sentinelmesh.recordings;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public class RecordingSegmentResource 
{
	private final Resource resource;
	private final MediaType mediaType;
	private final String filename;
	private final long contentLength;
	
	public RecordingSegmentResource(
			Resource resource,
			MediaType mediaType,
			String filename,
			long contentLength
			)
	{
		this.resource=resource;
		this.mediaType=mediaType;
		this.filename=filename;	
		this.contentLength=contentLength;
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
	
	public long getContentLength()
	{
		return contentLength;
	}
}
