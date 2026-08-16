package com.sentinelmesh.snapshots;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sentinelmesh.snapshots")
public class SnapshotStorageProperties 
{
	private String storageRoot;
	private long maxFileSizeBytes;
	private List<String> allowedContentTypes;
	
	public SnapshotStorageProperties()
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
	
	public void setStorageRoot(String storageRoot)
	{
		if(storageRoot==null || storageRoot.isBlank())
			throw new IllegalArgumentException("Storage Root cannot be null or blank");
		
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
}