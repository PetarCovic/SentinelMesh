package com.sentinelmesh.edge.audio;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AudioRingBuffer 
{
	private final ArrayDeque<AudioSampleBuffer> deque;
	private final long retentionNanos;
	private final int maxChunks;
	
	public AudioRingBuffer(long retentionNanos, int maxChunks)
	{
		if(retentionNanos<=0)
			throw new IllegalArgumentException("retentionNanos must be positive");
		
		if(maxChunks<=0)
			throw new IllegalArgumentException("maxChunks must be positive");
	
		deque=new ArrayDeque<>();
		this.retentionNanos=retentionNanos;
		this.maxChunks=maxChunks;
	}
	
	public void add(AudioSampleBuffer chunk)
	{
		while(!deque.isEmpty())
		{
			if(deque.getFirst().getCaptureStartNanos()
					+deque.getFirst().getDurationNanos()
					<=chunk.getCaptureStartNanos()
					+chunk.getDurationNanos()-retentionNanos)
				deque.removeFirst();
			else
				break;
		}
		
		deque.addLast(chunk);
		
		while(deque.size()>maxChunks)
			deque.removeFirst();
	}
	
	public List<AudioRange> snapshot(long startInclusiveNanos, long endExclusiveNanos)
	{
		List<AudioRange> ranges=new ArrayList<>();
		
		for(AudioSampleBuffer chunk : deque)
		{
			AudioRange range=new AudioRange(
					chunk, 
					0,
					chunk.getFrameCount()
					);
			
			Optional<AudioRange> optionalRange=range.slice(startInclusiveNanos, endExclusiveNanos);
			
			if(!optionalRange.isEmpty())
				ranges.add(optionalRange.get());
		}
		
		return ranges;
	}
	
	public Optional<Long> getLatestEndNanos()
	{
		if(deque.isEmpty())
			return Optional.empty();
		
		return Optional.of(deque.getLast().getCaptureStartNanos()
				+deque.getLast().getDurationNanos());
	}
	
	public void clear()
	{
		deque.clear();
	}
}
