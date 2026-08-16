package com.sentinelmesh.edge.live;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.sentinelmesh.edge.camera.Frame;
import com.sentinelmesh.edge.client.LiveFrameUploadClient;
import com.sentinelmesh.edge.media.SnapshotEncoder;

public class LiveFramePublisher implements AutoCloseable
{
	private final LiveFrameUploadClient uploadClient;
	private final SnapshotEncoder encoder;
	private final UUID deviceId;
	private final String apiKey;
	private final int livePreviewFps;
	private final long minimumFrameIntervalNanos;

	private final AtomicReference<byte[]> latestFrame=new AtomicReference<>();

	private final Object frameAvailableMonitor=new Object();

	private volatile boolean running = false;
	private volatile Thread workerThread;

	private long lastAcceptedFrameNanos = 0;

	public LiveFramePublisher(
			LiveFrameUploadClient uploadClient,
			SnapshotEncoder encoder,
			UUID deviceId,
			String apiKey,
			int livePreviewFps)
	{
		if(uploadClient == null)
			throw new IllegalArgumentException("UploadClient cannot be null");

		if(encoder == null)
			throw new IllegalArgumentException("SnapshotEncoder cannot be null");

		if(deviceId == null)
			throw new IllegalArgumentException("DeviceId cannot be null");

		if(apiKey == null || apiKey.isBlank())
			throw new IllegalArgumentException("ApiKey cannot be null or blank");

		if(livePreviewFps <= 0)
			throw new IllegalArgumentException("LivePreviewFps must be greater than 0");

		this.uploadClient = uploadClient;
		this.encoder = encoder;
		this.deviceId = deviceId;
		this.apiKey = apiKey;
		this.livePreviewFps = livePreviewFps;

		this.minimumFrameIntervalNanos =
				1_000_000_000L / livePreviewFps;
	}

	public void start()
	{
		synchronized(this)
		{
			if(running)
				return;

			running = true;

			workerThread = new Thread(this::runUploadLoop, "live-frame-publisher");

			workerThread.start();
		}
	}

	public void acceptFrame(Frame frame)
	{
		if(frame == null || frame.isEmpty())
			throw new IllegalArgumentException("Frame cannot be null or empty");

		if(!running)
			return;

		long now = System.nanoTime();

		synchronized(this)
		{
			if(lastAcceptedFrameNanos != 0 
					&& now - lastAcceptedFrameNanos < minimumFrameIntervalNanos)
				return;

			lastAcceptedFrameNanos = now;
		}

		byte[] imageBytes = encoder.encodeJpeg(frame);

		latestFrame.set(imageBytes);

		synchronized(frameAvailableMonitor)
		{
			frameAvailableMonitor.notifyAll();
		}
	}

	private void runUploadLoop()
	{
		while(running)
		{
			byte[] imageBytes = latestFrame.getAndSet(null);

			if(imageBytes == null)
			{
				waitForFrame();
				continue;
			}

			try
			{
				uploadClient.uploadLiveFrame(deviceId, apiKey, imageBytes);
			}
			catch(Exception ex)
			{
				System.err.println("Failed to upload live frame: "+ ex.getMessage());
			}
		}
	}

	private void waitForFrame()
	{
		synchronized(frameAvailableMonitor)
		{
			if(!running || latestFrame.get() != null)
				return;

			try
			{
				frameAvailableMonitor.wait();
			}
			catch(InterruptedException ex)
			{
				Thread.currentThread().interrupt();
			}
		}
	}

	public void stop()
	{
		Thread thread;

		synchronized(this)
		{
			if(!running)
				return;

			running = false;
			thread = workerThread;
		}

		latestFrame.set(null);

		synchronized(frameAvailableMonitor)
		{
			frameAvailableMonitor.notifyAll();
		}

		if(thread != null && thread != Thread.currentThread())
		{
			thread.interrupt();

			try
			{
				thread.join();
			}
			catch(InterruptedException ex)
			{
				Thread.currentThread().interrupt();
			}
		}

		synchronized(this)
		{
			if(workerThread == thread)
				workerThread = null;
		}
	}

	@Override
	public void close()
	{
		stop();
	}

	public boolean isRunning()
	{
		return running;
	}

	public int getLivePreviewFps()
	{
		return livePreviewFps;
	}
}