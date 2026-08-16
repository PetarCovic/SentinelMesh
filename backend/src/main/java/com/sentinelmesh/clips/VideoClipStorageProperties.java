package com.sentinelmesh.clips;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="sentinelmesh.clips")
public class VideoClipStorageProperties 
{
	private String storageRoot;
	private long maxFileSizeBytes;
	private List<String> allowedContentTypes;
	private int defaultDurationSeconds;
	private int defaultWidth;
	private int defaultHeight;
	
	public VideoClipStorageProperties()
	{
		allowedContentTypes=List.of();
	}
	
	public String getStorageRoot()
	{
		return storageRoot;
	}
	
	public long getMaxFileSizeBytes()
	{
		return maxFileSizeBytes;
	}
	
	public List<String> getAllowedContentTypes()
	{
		return allowedContentTypes;
	}
	
	public int getDefaultDurationSeconds()
	{
		return defaultDurationSeconds;
	}
	
	public int getDefaultWidth()
	{
		return defaultWidth;
	}
	
	public int getDefaultHeight()
	{
		return defaultHeight;
	}
	
	public void setStorageRoot(String storageRoot)
	{
		if(storageRoot==null || storageRoot.isBlank())
			throw new IllegalArgumentException("StorageRoot cannot be null or blank");
		
		this.storageRoot=storageRoot;
	}
	
	public void setMaxFileSizeBytes(long maxFileSizeBytes)
	{
		if(maxFileSizeBytes<=0)
			throw new IllegalArgumentException("Max file size must be greater than 0");
		
		this.maxFileSizeBytes=maxFileSizeBytes;
	}
	
	public void setAllowedContentTypes(List<String> allowedContentTypes)
	{
		if(allowedContentTypes==null)
			throw new IllegalArgumentException("AllowedContentTypes cannot be null");
		
		this.allowedContentTypes=allowedContentTypes;
	}
	
	public void setDefaultDurationSeconds(int defaultDurationSeconds)
	{
		if(defaultDurationSeconds<=0)
			throw new IllegalArgumentException("Default duration must be greater than 0");
		
		this.defaultDurationSeconds=defaultDurationSeconds;
	}
	
	public void setDefaultWidth(int defaultWidth)
	{
		if(defaultWidth<0)
			throw new IllegalArgumentException("Default width must be greater than or equal to 0");
		
		this.defaultWidth=defaultWidth;
	}
	
	public void setDefaultHeight(int defaultHeight)
	{
		if(defaultHeight<0)
			throw new IllegalArgumentException("Default height must be greater than or equal to 0");
		
		this.defaultHeight=defaultHeight;
	}
}
