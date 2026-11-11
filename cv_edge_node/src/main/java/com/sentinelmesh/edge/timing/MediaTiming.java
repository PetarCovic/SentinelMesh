package com.sentinelmesh.edge.timing;

public class MediaTiming 
{
	public static long framesToNanos(long frames, int sampleRate)
	{
		return (frames/sampleRate)*1_000_000_000L+(frames%sampleRate)*1_000_000_000L/sampleRate;
	}
	
	public static long videoTick(long captureNanos, long originNanos, double fps)
	{
		return Math.round((captureNanos-originNanos)*(fps/1_000_000_000.0)); 
	}
	
	public static long audioFrameAt(long captureNanos, long originNanos, int sampleRate)
	{
		return Math.round((captureNanos-originNanos)*(sampleRate/1_000_000_000.0));
	}
}
