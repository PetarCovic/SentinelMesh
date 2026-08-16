package com.sentinelmesh.recordings;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="sentinelmesh.recordings")
public class RecordingSegmentStorageProperties 
{
	private String storageRoot;
	private long maxFileSizeBytes;
	private List<String> allowedContentTypes;
	private int defaultDurationSeconds;
	private int defaultWidth;
	private int defaultHeight;
	
	public RecordingSegmentStorageProperties()
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
			throw new IllegalArgumentException("MaxFileSizeBytes must be greater than 0");
		
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
			throw new IllegalArgumentException("DefaultDurationSeconds must be greater than 0");
		
		this.defaultDurationSeconds=defaultDurationSeconds;
	}
	
	public void setDefaultWidth(int defaultWidth)
	{
		if(defaultWidth<0)
			throw new IllegalArgumentException("DefaultWidth cannot be negative");
		
		this.defaultWidth=defaultWidth;
	}
	
	public void setDefaultHeight(int defaultHeight)
	{
		if(defaultHeight<0)
			throw new IllegalArgumentException("DefaultHeight cannot be negative");
		
		this.defaultHeight=defaultHeight;
	}
}