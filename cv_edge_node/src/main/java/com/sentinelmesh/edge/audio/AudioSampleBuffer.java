package com.sentinelmesh.edge.audio;

import java.util.Arrays;

public final class AudioSampleBuffer
{
	private final byte[] pcmBytes;
	private final int sampleRate;
	private final int channels;
	private final long captureStartNanos;
	
	public AudioSampleBuffer(
			byte[] source, 
			int bytesRead, 
			int sampleRate,
			int channels,
			long captureStartNanos
			)
	{
		if(source==null)
			throw new IllegalArgumentException("source cannot be null");
		
		if(bytesRead<1 || bytesRead>source.length)
			throw new IllegalArgumentException("bytesRead must be between 1 and "+source.length);
	
		if(sampleRate<=0)
			throw new IllegalArgumentException("sampleRate must be positive");
		
		if(channels!=1 && channels!=2)
			throw new IllegalArgumentException("channels must be 1 or 2");
	
		if(bytesRead%(2*channels)!=0)
			throw new IllegalArgumentException("bytesRead must be multiple of audio frame size");
		
		this.pcmBytes=Arrays.copyOf(source, bytesRead);
		this.sampleRate=sampleRate;
		this.channels=channels;
		this.captureStartNanos=captureStartNanos;
	}
	
	public byte[] copyPcmBytes()
	{
		return Arrays.copyOf(pcmBytes, pcmBytes.length);
	}
	
	public int getSampleRate()
	{
		return sampleRate;
	}
	
	public int getChannels()
	{
		return channels;
	}
	
	public long getCaptureStartNanos()
	{
		return captureStartNanos;
	}
	
	public int getFrameCount()
	{
		return pcmBytes.length/(2*channels);
	}
	
	public long getDurationNanos()
	{
		return getFrameCount()*1_000_000_000L/sampleRate;
	}
}