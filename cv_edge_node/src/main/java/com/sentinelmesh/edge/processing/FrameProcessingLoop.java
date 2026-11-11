package com.sentinelmesh.edge.processing;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import com.sentinelmesh.edge.audio.AudioCaptureWorker;
import com.sentinelmesh.edge.audio.AudioSampleBuffer;
import com.sentinelmesh.edge.camera.Frame;
import com.sentinelmesh.edge.camera.FrameSource;
import com.sentinelmesh.edge.config.EdgeNodeConfig;
import com.sentinelmesh.edge.detection.DetectionResult;
import com.sentinelmesh.edge.live.LiveFramePublisher;
import com.sentinelmesh.edge.media.EventClipCoordinator;
import com.sentinelmesh.edge.media.FrameAnnotationRenderer;
import com.sentinelmesh.edge.recording.ContinuousRecordingSubsystem;

public class FrameProcessingLoop
{
	private final FrameSource fs;
	private final FrameProcessor frameProcessor;
	private final EdgeNodeConfig config;
	private final ContinuousRecordingSubsystem recordingSubsystem;
	private final LiveFramePublisher liveFramePublisher;
	private final FrameAnnotationRenderer frameAnnotationRenderer;
	private volatile Thread processingThread;
	private final AudioCaptureWorker audioCaptureWorker;
	private final EventClipCoordinator eventClipCoordinator;

	private final CountDownLatch loopFinished = new CountDownLatch(1);
	private volatile boolean running=false;

	public FrameProcessingLoop(
			FrameSource fs,
			FrameProcessor frameProcessor,
			EdgeNodeConfig config,
			ContinuousRecordingSubsystem recordingSubsystem,
			FrameAnnotationRenderer frameAnnotationRenderer,
			LiveFramePublisher liveFramePublisher,
			AudioCaptureWorker audioCaptureWorker,
			EventClipCoordinator eventClipCoordinator
			)
	{
		if(fs==null)
			throw new IllegalArgumentException("Frame source cannot be null");

		if(frameProcessor==null)
			throw new IllegalArgumentException("Frame Processor cannot be null");

		if(config==null)
			throw new IllegalArgumentException("EdgeNodeConfig cannot be null");

		if(recordingSubsystem==null)
			throw new IllegalArgumentException("RecordingSubsystem cannot be null");

		if(liveFramePublisher==null)
			throw new IllegalArgumentException("LiveFramePublisher cannot be null");

		if(frameAnnotationRenderer==null)
			throw new IllegalArgumentException("FrameAnnotationRenderer cannot be null");
		
		if(eventClipCoordinator==null)
			throw new IllegalArgumentException("eventClipCoordinator cannot be null");

		this.fs=fs;
		this.frameProcessor=frameProcessor;
		this.config=config;
		this.recordingSubsystem=recordingSubsystem;
		this.liveFramePublisher=liveFramePublisher;
		this.frameAnnotationRenderer=frameAnnotationRenderer;
		this.audioCaptureWorker=audioCaptureWorker;
		this.eventClipCoordinator=eventClipCoordinator;
	}

	public void start()
	{
	    if(running)
	        return;

	    running = true;
	    processingThread = Thread.currentThread();

	    try
	    {
	        runLoop();
	    }
	    finally
	    {
	    	running=false;
	        processingThread = null;
	        loopFinished.countDown();
	    }
	}

	public void stop()
	{
	    running = false;

	    Thread thread = processingThread;

	    if(thread != null && thread != Thread.currentThread())
	        thread.interrupt();
	}

	public void awaitTermination()
	{
	    Thread thread = processingThread;
	    if (thread == null || thread == Thread.currentThread())
	        return;

	    boolean interrupted = false;
	    while (true)
	    {
	        try
	        {
	            loopFinished.await();
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

	private void runLoop()
	{
		Frame frame=new Frame();

		try
		{
			while(running)
			{
				long frameStartNanos = System.nanoTime();

				boolean read=fs.read(frame);

				if(!read)
				{
					running=false;
					break;
				}

				List<AudioSampleBuffer> audioChunks=List.of();
				boolean audioRunning=false;
				
				if(audioCaptureWorker!=null)
				{
					audioRunning=audioCaptureWorker.isRunning();
					audioChunks=audioCaptureWorker.drainAvailable();
				}
				
				try
				{
					recordingSubsystem.acceptFrame(
							frame,
							audioChunks,
							audioRunning);
				}
				catch(Exception ex)
				{
					System.err.println("Recording Subsystem failed: "+ex.getMessage());
				}

				try
				{
					eventClipCoordinator.acceptFrame(frame, audioChunks, audioRunning);
				}
				catch(Exception ex)
				{
					System.err.println("EventClipCoordinator failed: "+ex.getMessage());
				}
				
				List<DetectionResult> detections=List.of();
				try
				{
					detections=frameProcessor.process(frame);
				}catch(Exception ex)
				{
					System.err.println("Frame Processing failed: "+ex.getMessage());
				}

				publishLiveFrame(frame, detections);

				sleepForTargetFps(frameStartNanos);
			}
		}
		finally
		{
			frame.close();
		}
	}

	private void sleepForTargetFps(long frameStartNanos)
	{
		int fps = config.getTargetFps();

		long targetFrameDurationNanos =
				1_000_000_000L / fps;

		long elapsedNanos =
				System.nanoTime() - frameStartNanos;

		long remainingNanos =
				targetFrameDurationNanos - elapsedNanos;

		if(remainingNanos <= 0)
			return;

		long sleepMillis =
				remainingNanos / 1_000_000L;

		int sleepNanos =
				(int)(remainingNanos % 1_000_000L);

		try
		{
			Thread.sleep(sleepMillis, sleepNanos);
		}
		catch(InterruptedException ex)
		{
			running = false;
			Thread.currentThread().interrupt();
		}
	}

	private void publishLiveFrame(Frame frame, List<DetectionResult> detections)
	{
	    Frame copy = null;
	    Frame frameToPublish = frame;

	    try
	    {
	        if(config.isLiveDetectionOverlayEnabled() && !detections.isEmpty())
	        {
	            try
	            {
	                copy = frameAnnotationRenderer.renderCopy(frame, detections);
	                frameToPublish = copy;
	            }
	            catch(Exception ex)
	            {
	                System.err.println("Frame annotation failed: " + ex.getMessage());
	            }
	        }

	        liveFramePublisher.acceptFrame(frameToPublish);
	    }
	    catch(Exception ex)
	    {
	        System.err.println("LiveFramePublisher failed: " + ex.getMessage());
	    }
	    finally
	    {
	        if(copy != null)
	            copy.close();
	    }
	}
}
