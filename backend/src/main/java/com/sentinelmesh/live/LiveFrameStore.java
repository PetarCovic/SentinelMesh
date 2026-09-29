package com.sentinelmesh.live;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class LiveFrameStore
{
	private final ConcurrentHashMap<UUID, LiveFrameData> store;
	
	public LiveFrameStore()
	{
		store=new ConcurrentHashMap<>();
	}
	
	public void put(LiveFrameData frame)
	{
		if(frame==null)
			throw new IllegalArgumentException("LiveFrameData cannot be null");
		
		store.put(frame.getDeviceId(), frame);
	}
	
	public Optional<LiveFrameData> get(UUID deviceId)
	{
		if(deviceId==null)
			throw new IllegalArgumentException("DeviceId cannot be null");
		
		LiveFrameData data=store.get(deviceId);
		
		if(data!=null)
			return Optional.of(data);
		else
			return Optional.empty();
	}
	
	public void remove(UUID deviceId)
	{
		if(deviceId==null)
			throw new IllegalArgumentException("DeviceId cannot be null");
		
		store.remove(deviceId);
	}
	
	public boolean remove(UUID deviceId, LiveFrameData frame)
	{
		if(deviceId==null)
			throw new IllegalArgumentException("DeviceId cannot be null");
		
		if(frame==null)
			throw new IllegalArgumentException("LiveFrameData cannot be null");
		
		return store.remove(deviceId, frame);
	}
	
	public boolean contains(UUID deviceId)
	{
		if(deviceId==null)
			throw new IllegalArgumentException("DeviceId cannot be null");
		
		return store.contains(deviceId);
	}
	
	public void clear()
	{
		store.clear();
	}
}