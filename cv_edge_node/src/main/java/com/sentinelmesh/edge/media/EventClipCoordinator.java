package com.sentinelmesh.edge.media;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.sentinelmesh.edge.audio.AudioRange;
import com.sentinelmesh.edge.audio.AudioRingBuffer;
import com.sentinelmesh.edge.audio.AudioSampleBuffer;
import com.sentinelmesh.edge.camera.Frame;
import com.sentinelmesh.edge.timing.MediaTiming;

public final class EventClipCoordinator implements AutoCloseable
{
    private static final long NANOS_PER_SECOND = 1_000_000_000L;
    private static final long AUDIO_GRACE_NANOS = 250_000_000L;
    private static final int AUDIO_CHUNKS_PER_SECOND = 50;
    private static final int MAX_IN_FLIGHT_CLIPS = 2;
    private static final long SHUTDOWN_WAIT_SECONDS = 10;

    private final long preRollNanos;
    private final long postRollNanos;
    private final int clipFps;
    private final int maxFramesPerClip;
    private final ClipHandler handler;
    private final FrameRingBuffer frameHistory;
    private final AudioRingBuffer audioHistory;
    private final ThreadPoolExecutor worker;

    private final Deque<PendingEventClip> pendingClips = new ArrayDeque<>();
    private final Set<UUID> inFlight = new HashSet<>();
    private boolean finished;

    public EventClipCoordinator(
            int preSeconds,
            int postSeconds,
            int captureFps,
            int clipFps,
            ClipHandler handler)
    {
        if (preSeconds < 0 || postSeconds <= 0)
            throw new IllegalArgumentException("preSeconds must be nonnegative and postSeconds must be positive");
        if (captureFps <= 0 || clipFps <= 0)
            throw new IllegalArgumentException("captureFps and clipFps must be positive");

        this.handler = Objects.requireNonNull(handler, "handler cannot be null");
        this.preRollNanos = preSeconds * NANOS_PER_SECOND;
        this.postRollNanos = postSeconds * NANOS_PER_SECOND;
        this.clipFps = clipFps;

        long clipSeconds = (long) preSeconds + postSeconds;
        int historyFrames = Math.toIntExact((long) preSeconds * captureFps + 1);
        this.maxFramesPerClip = Math.toIntExact(clipSeconds * captureFps + 1);
        this.frameHistory = new FrameRingBuffer(historyFrames);

        // Retain the whole clip window, with room for the audio grace period and a late batch.
        long audioRetentionNanos = preRollNanos + postRollNanos + AUDIO_GRACE_NANOS + NANOS_PER_SECOND;
        int maxAudioChunks = Math.toIntExact((clipSeconds + 2) * AUDIO_CHUNKS_PER_SECOND + 1);
        this.audioHistory = new AudioRingBuffer(audioRetentionNanos, maxAudioChunks);

        this.worker = new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(MAX_IN_FLIGHT_CLIPS),
                task -> {
                    Thread thread = new Thread(task, "sentinelmesh-event-clip");
                    thread.setDaemon(true);
                    return thread;
                });
    }

    public synchronized boolean requestClip(UUID eventId, long triggerNanos)
    {
        Objects.requireNonNull(eventId, "eventId cannot be null");
        if (finished || inFlight.size() >= MAX_IN_FLIGHT_CLIPS || !inFlight.add(eventId))
            return false;

        List<Frame> frames = null;
        boolean accepted = false;
        try
        {
            frames = new ArrayList<>(frameHistory.snapshot());
            Iterator<Frame> iterator = frames.iterator();
            while (iterator.hasNext())
            {
                Frame frame = iterator.next();
                long ageNanos = triggerNanos - frame.getCaptureNanos();
                if (ageNanos < 0 || ageNanos > preRollNanos)
                {
                    iterator.remove();
                    closeFrame(frame);
                }
            }

            if (frames.isEmpty())
                return false;

            pendingClips.addLast(new PendingEventClip(
                    eventId, triggerNanos, triggerNanos + postRollNanos, frames));
            accepted = true;
            return true;
        }
        finally
        {
            if (!accepted)
                releaseClip(eventId, frames == null ? List.of() : frames);
        }
    }

    public synchronized void acceptFrame(
            Frame frame, List<AudioSampleBuffer> audioChunks, boolean audioRunning)
    {
        if (finished)
            return;

        Objects.requireNonNull(audioChunks, "audioChunks cannot be null");
        frameHistory.add(frame);
        addAudio(audioChunks);

        long captureNanos = frame.getCaptureNanos();
        for (PendingEventClip clip : pendingClips)
        {
            Frame lastFrame = clip.frames.get(clip.frames.size() - 1);
            if (captureNanos - clip.triggerNanos > 0
                    && captureNanos - clip.requestedEndNanos < 0
                    && captureNanos - lastFrame.getCaptureNanos() > 0
                    && clip.frames.size() < maxFramesPerClip)
            {
                clip.frames.add(frame.copy());
            }
        }

        finishReadyClips(captureNanos, audioRunning);
    }

    private void addAudio(List<AudioSampleBuffer> chunks)
    {
        for (AudioSampleBuffer chunk : chunks)
            audioHistory.add(chunk);
    }

    private void finishReadyClips(long videoCaptureNanos, boolean audioRunning)
    {
        if (pendingClips.isEmpty())
            return;

        long nowNanos = System.nanoTime();
        Optional<Long> latestAudioEnd = audioHistory.getLatestEndNanos();
        Iterator<PendingEventClip> iterator = pendingClips.iterator();
        while (iterator.hasNext())
        {
            PendingEventClip clip = iterator.next();
            if (videoCaptureNanos - clip.requestedEndNanos < 0)
                continue;

            if (clip.audioWaitStartedNanos == null)
                clip.audioWaitStartedNanos = nowNanos;

            long clipEndNanos = clipEndNanos(clip);
            boolean audioCaughtUp = latestAudioEnd.isPresent()
                    && latestAudioEnd.get() - clipEndNanos >= 0;
            boolean graceExpired = nowNanos - clip.audioWaitStartedNanos >= AUDIO_GRACE_NANOS;
            if (!audioRunning || audioCaughtUp || graceExpired)
            {
                iterator.remove();
                submitClip(clip);
            }
        }
    }

    private long clipEndNanos(PendingEventClip clip)
    {
        long originNanos = clip.frames.get(0).getCaptureNanos();
        long lastCaptureNanos = clip.frames.get(clip.frames.size() - 1).getCaptureNanos();
        long lastTick = MediaTiming.videoTick(lastCaptureNanos, originNanos, clipFps);
        long videoDurationNanos = Math.max(1L,
                Math.round((lastTick + 1.0) * NANOS_PER_SECOND / clipFps));
        long requestedDurationNanos = clip.requestedEndNanos - originNanos;
        return originNanos + Math.min(videoDurationNanos, requestedDurationNanos);
    }

    private void submitClip(PendingEventClip clip)
    {
        boolean submitted = false;
        try
        {
            long startNanos = clip.frames.get(0).getCaptureNanos();
            List<AudioRange> audioRanges = List.copyOf(audioHistory.snapshot(startNanos, clipEndNanos(clip)));
            worker.execute(new ClipJob(clip.eventId, List.copyOf(clip.frames), audioRanges));
            submitted = true;
        }
        catch (RuntimeException ex)
        {
            System.err.println("Could not queue event clip " + clip.eventId + ": " + ex.getMessage());
        }
        finally
        {
            if (!submitted)
                releaseClip(clip.eventId, clip.frames);
        }
    }

    public synchronized void finish(List<AudioSampleBuffer> finalAudioChunks)
    {
        if (finished)
            return;

        Objects.requireNonNull(finalAudioChunks, "finalAudioChunks cannot be null");
        finished = true;
        try
        {
            addAudio(finalAudioChunks);
            while (!pendingClips.isEmpty())
                submitClip(pendingClips.removeFirst());
        }
        finally
        {
            try
            {
                while (!pendingClips.isEmpty())
                {
                    PendingEventClip clip = pendingClips.removeFirst();
                    releaseClip(clip.eventId, clip.frames);
                }
                frameHistory.close();
            }
            finally
            {
                audioHistory.clear();
                worker.shutdown();
            }
        }
    }

    @Override
    public void close()
    {
        try
        {
            finish(List.of());
        }
        finally
        {
            // The worker must be able to take the coordinator lock to release its clip slot.
            try
            {
                if (!worker.awaitTermination(SHUTDOWN_WAIT_SECONDS, TimeUnit.SECONDS))
                    cancelQueuedClips();
            }
            catch (InterruptedException ex)
            {
                try
                {
                    cancelQueuedClips();
                }
                finally
                {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void cancelQueuedClips()
    {
        for (Runnable task : worker.shutdownNow())
        {
            if (task instanceof ClipJob job)
                job.dispose();
        }
    }

    private void releaseClip(UUID eventId, List<Frame> frames)
    {
        try
        {
            for (Frame frame : frames)
                closeFrame(frame);
        }
        finally
        {
            synchronized (this)
            {
                inFlight.remove(eventId);
            }
        }
    }

    private static void closeFrame(Frame frame)
    {
        try
        {
            frame.close();
        }
        catch (RuntimeException ex)
        {
            System.err.println("Could not release event clip frame: " + ex.getMessage());
        }
    }

    @FunctionalInterface
    public interface ClipHandler
    {
        void handle(UUID eventId, List<Frame> frames, List<AudioRange> audioRanges) throws Exception;
    }

    private static final class PendingEventClip
    {
        private final UUID eventId;
        private final long triggerNanos;
        private final long requestedEndNanos;
        private final List<Frame> frames;
        private Long audioWaitStartedNanos;

        private PendingEventClip(UUID eventId, long triggerNanos, long requestedEndNanos, List<Frame> frames)
        {
            this.eventId = eventId;
            this.triggerNanos = triggerNanos;
            this.requestedEndNanos = requestedEndNanos;
            this.frames = frames;
        }
    }

    private final class ClipJob implements Runnable
    {
        private final UUID eventId;
        private final List<Frame> frames;
        private final List<AudioRange> audioRanges;

        private ClipJob(UUID eventId, List<Frame> frames, List<AudioRange> audioRanges)
        {
            this.eventId = eventId;
            this.frames = frames;
            this.audioRanges = audioRanges;
        }

        @Override
        public void run()
        {
            try
            {
                handler.handle(eventId, frames, audioRanges);
            }
            catch (InterruptedException ex)
            {
                Thread.currentThread().interrupt();
            }
            catch (Exception ex)
            {
                System.err.println("Event clip " + eventId + " failed: " + ex.getMessage());
            }
            finally
            {
                dispose();
            }
        }

        private void dispose()
        {
            releaseClip(eventId, frames);
        }
    }
}