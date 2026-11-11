package com.sentinelmesh.edge.audio;

import java.util.Optional;

public record AudioRange(
		AudioSampleBuffer chunk,
		int offsetFrames, 
		int frameCount
		)
{
	public AudioRange
	{
		if(chunk==null)
			throw new IllegalArgumentException("chunk cannot be null");
		
		if(offsetFrames<0)
			throw new IllegalArgumentException("offsetFrames cannot be negative");
		
		if(frameCount<=0)
			throw new IllegalArgumentException("frameCount must be positive");
	
		if(frameCount>chunk.getFrameCount()-offsetFrames)
			throw new IllegalArgumentException("frameCount must be at most chunk.getFrameCount()-offsetFrames");
	}
	
	public static int framesBefore(AudioSampleBuffer chunk, long cutoffNanos)
	{
		long elapsedNanos=cutoffNanos-chunk.getCaptureStartNanos();
		
		if(elapsedNanos<=0)
			return 0;
		
		if(elapsedNanos>chunk.getDurationNanos())
			return chunk.getFrameCount();
		
		long duration=(long)Math.ceil((
						elapsedNanos*chunk.getSampleRate())/1_000_000_000.0
				);
		
		if(duration>chunk.getFrameCount())
			return chunk.getFrameCount();
		
		return (int)duration;
	}
	
	public Optional<AudioRange> slice(
			long startInclusiveNanos,
			long endExclusiveNanos
			)
	{
		int startFrame=framesBefore(chunk, startInclusiveNanos);
		int endFrame=framesBefore(chunk, endExclusiveNanos);
		
		int start=Math.max(startFrame, offsetFrames);
		int end=Math.min(endFrame, offsetFrames+frameCount);
		
		if(end<=start)
			return Optional.empty();
		
		return Optional.of(new AudioRange(
				chunk,
				start,
				end-start
				));
	}
}