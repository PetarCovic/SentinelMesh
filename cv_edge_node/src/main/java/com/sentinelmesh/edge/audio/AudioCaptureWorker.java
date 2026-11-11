package com.sentinelmesh.edge.audio;

import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.sentinelmesh.edge.timing.MediaTiming;

public final class AudioCaptureWorker implements AutoCloseable
{
	private final AudioSource source;
	private final int sampleRate;
	private final int channels;
	
	private Thread captureThread;
	private final ArrayDeque<AudioSampleBuffer> deque=new ArrayDeque<>();
	private final CountDownLatch startupComplete = new CountDownLatch(1);
	private volatile boolean running;
	private volatile boolean closed;
	
	public AudioCaptureWorker(
			AudioSource source, 
			int sampleRate, 
			int channels
			)
	{
		if(source==null)
			throw new IllegalArgumentException("source cannot be null");
		
		if(sampleRate<=0)
			throw new IllegalArgumentException("sampleRate must be positive");
		
		if(channels!=1 && channels!=2)
			throw new IllegalArgumentException("channels must be 1 or 2");
		
		this.source=source;
		this.sampleRate=sampleRate;
		this.channels=channels;
	}
	
	public synchronized void start()
	{
		if(closed)
			throw new IllegalStateException("AudioCaptureWorker is closed");
	
		if(captureThread!=null)
			return;
		
		captureThread=new Thread(this::runCaptureLoop, "sentinelmesh-audio-capture");
		captureThread.setDaemon(false);
		captureThread.start();
	}
	
	public AudioSampleBuffer poll()
	{
		synchronized(deque)
		{
			return deque.pollFirst();
		}
	}
	
	public boolean isRunning()
	{
		return running && !closed;
	}
	
	public boolean isClosed()
	{
		return closed;
	}
	
	public List<AudioSampleBuffer> drainAvailable()
	{		
		synchronized(deque)
		{
			List<AudioSampleBuffer> audioSampleBufferList=
					List.copyOf(deque);
			deque.clear();
			
			return audioSampleBufferList;
		}
	}
	
	public boolean awaitStartup(long timeout, TimeUnit unit) throws InterruptedException
	{
	    return startupComplete.await(timeout, unit) && isRunning();
	}
	
	public void close()
	{
		Thread thread;
		
		synchronized(this)
		{
			if(closed)
				return;
			
			closed=true;
			
			thread=captureThread;
		}
		
		source.close();
		
		if (thread == null || thread == Thread.currentThread())
		    return;
		
		boolean interrupted = false;

		while (true)
		{
		    try
		    {
		        thread.join();
		        break;
		    }
		    catch (InterruptedException ex)
		    {
		        interrupted = true;
		    }
		}

		if (interrupted)
		    Thread.currentThread().interrupt();
	}
	
	private void runCaptureLoop()
	{
		try
		{
			if(!source.start())
			{
				System.out.println("Audio Unavailable");
				return;
			}
			
			running=true;
			startupComplete.countDown();
			
			int frameCount=Math.max(1, sampleRate/50);
			int bytesPerFrame=2*channels;
			byte[] buffer=new byte[frameCount*bytesPerFrame];
			
			long audioAnchorNanos = 0L;
			long framesCaptured = 0L;
			
			while(!closed)
			{
				int bytesRead=source.read(buffer, 0, buffer.length);
				long readEndNanos=System.nanoTime();
				
				if(bytesRead==-1)
					break;
				
				if(bytesRead==0)
				{
					try {
						Thread.sleep(10);
						continue;
					} catch (InterruptedException e) 
					{
						Thread.currentThread().interrupt();
						return;
					}
				}
				
				int framesInChunk = bytesRead / bytesPerFrame;

				if (framesCaptured == 0)
				{
				    audioAnchorNanos = readEndNanos
				            - MediaTiming.framesToNanos(framesInChunk, sampleRate);
				}

				long captureStartNanos = audioAnchorNanos
				        + MediaTiming.framesToNanos(framesCaptured, sampleRate);
				
				AudioSampleBuffer chunk = new AudioSampleBuffer(
				        buffer, 
				        bytesRead,
				        sampleRate, 
				        channels,
				        captureStartNanos
				        );
				
				framesCaptured+=framesInChunk;
				
				synchronized(deque)
				{
					if(deque.size()==50)
						deque.removeFirst();
					
					deque.addLast(chunk);
				}
			}
		}
		catch(IllegalArgumentException
				| SecurityException e)
		{
			System.out.println("Audio Unavailable");
			return;
		}
		finally
		{
			running=false;
			startupComplete.countDown();
			source.close();
		}
	}
}