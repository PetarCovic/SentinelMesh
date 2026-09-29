package com.sentinelmesh.live;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.UUID;

public class LiveFrameData 
{
	private final UUID deviceId;
	private final byte[] jpegBytes;
	private final Instant receivedAt;
	
	public LiveFrameData(UUID deviceId, byte[] jpegBytes, Instant receivedAt)
	{
		if(deviceId==null)
			throw new IllegalArgumentException("DeviceId cannot be null");
		
		if(jpegBytes==null || jpegBytes.length==0)
			throw new IllegalArgumentException("jpegBytes cannot be null or empty");
		
		if(receivedAt==null)
			throw new IllegalArgumentException("ReceivedAt cannot be null");
		
		this.deviceId=deviceId;
		this.jpegBytes=jpegBytes.clone();
		this.receivedAt=receivedAt;
	}
	
	public UUID getDeviceId() 
	{
		return deviceId;
	}

	public byte[] getJpegBytes()
	{
		return jpegBytes.clone();
	}

	public Instant getReceivedAt()
	{
		return receivedAt;
	}
	
	public int getJpegSize()
	{
		return jpegBytes.length;
	}
	
	public void writeJpegTo(OutputStream outputStream) throws IOException
	{
		if(outputStream==null)
			throw new IllegalArgumentException("OutputStream cannot be null");
			
		outputStream.write(jpegBytes);
	}
}