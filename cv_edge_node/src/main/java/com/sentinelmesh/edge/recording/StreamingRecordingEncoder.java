package com.sentinelmesh.edge.recording;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import com.sentinelmesh.edge.audio.AudioRange;
import com.sentinelmesh.edge.audio.AudioSampleBuffer;
import com.sentinelmesh.edge.camera.Frame;
import com.sentinelmesh.edge.exceptions.RecordingSpoolException;
import com.sentinelmesh.edge.media.Mp4MediaWriter;
import com.sentinelmesh.edge.timing.MediaTiming;
import com.sentinelmesh.edge.util.FileUtil;

public class StreamingRecordingEncoder implements AutoCloseable
{
    private static final System.Logger LOG = System.getLogger(StreamingRecordingEncoder.class.getName());
    private static final long NANOS_PER_SECOND = 1_000_000_000L;
    private static final long CLOSING_AUDIO_GRACE_NANOS = 250_000_000L;
    private static final int MAX_PENDING_AUDIO_RANGES = 50;
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssSSS'Z'").withZone(ZoneOffset.UTC);

    private final RecordingSpoolDirectories spoolDirectories;
    private final RecordingSpoolService spoolService;
    private final UUID deviceId;
    private final long segmentDurationNanos;
    private final double configuredFPS;
    private final int sampleRate;
    private final int channels;
    private final double recordingAudioGainDb;
    private final Deque<AudioRange> pendingAudio = new ArrayDeque<>();

    private SegmentState activeSegment;
    private SegmentState closingSegment;
    private long closingCutoffNanos;
    private long closingSinceNanos;
    private Long latestAudioEndNanos;
    private Long lastVideoCaptureNanos;
    private boolean audioDisabledAfterFailure;
    private boolean finished;
    private boolean closed;

    public StreamingRecordingEncoder(
            RecordingSpoolDirectories spoolDirectories,
            RecordingSpoolService spoolService,
            UUID deviceId,
            int segmentDurationSeconds,
            double configuredFPS,
            int sampleRate,
            int channels,
            double recordingAudioGainDb)
    {
        this.spoolDirectories = Objects.requireNonNull(spoolDirectories, "spoolDirectories cannot be null");
        this.spoolService = Objects.requireNonNull(spoolService, "spoolService cannot be null");
        this.deviceId = Objects.requireNonNull(deviceId, "deviceId cannot be null");

        if (segmentDurationSeconds <= 0)
            throw new IllegalArgumentException("segmentDurationSeconds must be positive");
        if (!Double.isFinite(configuredFPS) || configuredFPS <= 0)
            throw new IllegalArgumentException("configuredFPS must be finite and positive");

        if(!Double.isFinite(recordingAudioGainDb)
				|| (recordingAudioGainDb<-24 || recordingAudioGainDb>24))
			throw new IllegalArgumentException("recordingAudioGainDb must between -24 and 24 Db");
        
        this.segmentDurationNanos = segmentDurationSeconds * NANOS_PER_SECOND;
        this.configuredFPS = configuredFPS;
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.recordingAudioGainDb=recordingAudioGainDb;
    }

    public synchronized List<RecordingSpoolEntry> acceptFrame(
            Frame frame,
            List<AudioSampleBuffer> audioChunks,
            boolean audioRunning,
            BooleanSupplier canOpenSegment)
    {
        if (closed || finished)
            throw new IllegalStateException("StreamingRecordingEncoder is finished");

        validateFrame(frame);
        Objects.requireNonNull(audioChunks, "audioChunks cannot be null");
        Objects.requireNonNull(canOpenSegment, "canOpenSegment cannot be null");

        long captureNanos = frame.getCaptureNanos();
        if (lastVideoCaptureNanos != null && captureNanos - lastVideoCaptureNanos < 0)
            throw new IllegalArgumentException("Video frames must arrive in capture order");
        lastVideoCaptureNanos = captureNanos;

        enqueueAudio(audioChunks);
        List<RecordingSpoolEntry> completed = new ArrayList<>(2);

        boolean rotate = activeSegment != null
                && captureNanos - activeSegment.startCaptureNanos >= segmentDurationNanos;

        // Free the closing slot before rotating another segment into it.
        finishClosingIfReady(audioRunning, rotate, completed);
        if (rotate)
        {
            closingSegment = activeSegment;
            activeSegment = null;
            closingCutoffNanos = captureNanos;
            closingSinceNanos = System.nanoTime();
            finishClosingIfReady(audioRunning, false, completed);
        }

        if (activeSegment == null)
        {
            try
            {
                if (canOpenSegment.getAsBoolean())
                    activeSegment = openSegment(frame, audioRunning && !audioDisabledAfterFailure);
            }
            catch (RuntimeException failure)
            {
                // Keep entries that were already completed in this call.
                LOG.log(System.Logger.Level.WARNING, "Could not start a recording segment", failure);
            }
        }

        if (activeSegment != null)
            writeFrame(activeSegment, frame);

        // Keep samples at and after this frame for a later video frame or rollover.
        // If no writer could open, consume the expired prefix without writing it.
        consumeAudioBefore(activeSegment, captureNanos);
        return List.copyOf(completed);
    }

    private void enqueueAudio(List<AudioSampleBuffer> chunks)
    {
        if (audioDisabledAfterFailure)
            return;

        for (AudioSampleBuffer chunk : chunks)
        {
            pendingAudio.addLast(new AudioRange(chunk, 0, chunk.getFrameCount()));
            long endNanos = chunk.getCaptureStartNanos() + chunk.getDurationNanos();
            if (latestAudioEndNanos == null || endNanos - latestAudioEndNanos > 0)
                latestAudioEndNanos = endNanos;

            if (pendingAudio.size() > MAX_PENDING_AUDIO_RANGES)
                pendingAudio.removeFirst();
        }
    }

    private void consumeAudioBefore(SegmentState segment, long cutoffNanos)
    {
        while (!pendingAudio.isEmpty())
        {
            AudioRange range = pendingAudio.peekFirst();
            int rangeEnd = range.offsetFrames() + range.frameCount();
            int split = Math.min(rangeEnd, AudioRange.framesBefore(range.chunk(), cutoffNanos));
            if (split <= range.offsetFrames())
                return;

            if (segment != null && segment.audioEnabled && !audioDisabledAfterFailure)
            {
                AudioRange toWrite = range.slice(segment.startCaptureNanos, cutoffNanos).orElse(null);
                if (toWrite != null)
                {
                    try
                    {
                        segment.writer.writeAudioFrames(
                                toWrite.chunk(), toWrite.offsetFrames(), toWrite.frameCount());
                    }
                    catch (RuntimeException failure)
                    {
                        audioDisabledAfterFailure = true;
                        pendingAudio.clear();
                        abandonSegment(segment, failure);
                        return;
                    }
                }
            }

            pendingAudio.removeFirst();
            if (split < rangeEnd)
            {
                pendingAudio.addFirst(new AudioRange(range.chunk(), split, rangeEnd - split));
                return;
            }
        }
    }

    private void finishClosingIfReady(
            boolean audioRunning, boolean force, List<RecordingSpoolEntry> completed)
    {
        SegmentState segment = closingSegment;
        if (segment == null)
            return;

        consumeAudioBefore(segment, closingCutoffNanos);
        if (closingSegment != segment)
            return; // An audio write failure already abandoned this writer.

        boolean audioReachedCutoff = latestAudioEndNanos != null
                && latestAudioEndNanos - closingCutoffNanos >= 0;
        boolean graceExpired = System.nanoTime() - closingSinceNanos >= CLOSING_AUDIO_GRACE_NANOS;
        if (force || !segment.audioEnabled || audioDisabledAfterFailure || !audioRunning
                || audioReachedCutoff || graceExpired)
        {
            finalizeSegment(segment, closingCutoffNanos, completed);
        }
    }

    private SegmentState openSegment(Frame firstFrame, boolean audioEnabled)
    {
        UUID segmentId = UUID.randomUUID();
        Instant startTime = firstFrame.getTimestamp();
        String filename = FILE_TIMESTAMP_FORMAT.format(startTime)
                + "_" + deviceId + "_" + segmentId + ".mp4";
        Path videoPath = FileUtil.normalizeAbsolute(spoolDirectories.getActive()).resolve(filename);

        Mp4MediaWriter writer;
        try
        {
            writer = openWriter(videoPath, firstFrame, audioEnabled);
        }
        catch (RuntimeException openFailure)
        {
            boolean pathCleared = quarantinePartial(videoPath, openFailure);
            if (!audioEnabled || !pathCleared)
                throw openFailure;

            LOG.log(System.Logger.Level.WARNING, "Audio setup failed; retrying with video only: " + videoPath,
                    openFailure);
            try
            {
                writer = openWriter(videoPath, firstFrame, false);
                audioEnabled = false;
            }
            catch (RuntimeException retryFailure)
            {
                if (retryFailure != openFailure)
                    retryFailure.addSuppressed(openFailure);
                quarantinePartial(videoPath, retryFailure);
                throw retryFailure;
            }
        }

        return new SegmentState(
                segmentId, videoPath, startTime, firstFrame.getCaptureNanos(), writer, audioEnabled);
    }

    private Mp4MediaWriter openWriter(Path path, Frame firstFrame, boolean audioEnabled)
    {
        return new Mp4MediaWriter(
                path, 
                firstFrame.getWidth(), 
                firstFrame.getHeight(), 
                configuredFPS,
                firstFrame.getCaptureNanos(), 
                sampleRate, 
                channels, 
                audioEnabled,
                recordingAudioGainDb
                );
    }

    private void writeFrame(SegmentState segment, Frame frame)
    {
        try
        {
            if (segment.writer.writeVideoFrame(frame))
            {
                segment.lastVideoTick = MediaTiming.videoTick(
                        frame.getCaptureNanos(), segment.startCaptureNanos, configuredFPS);
            }
        }
        catch (RuntimeException failure)
        {
            abandonSegment(segment, failure);
        }
    }

    private void finalizeSegment(SegmentState segment, long endCaptureNanos, List<RecordingSpoolEntry> completed)
    {
        if (segment.lastVideoTick < 0)
        {
            abandonSegment(segment, new RecordingSpoolException("Segment contained no written video frames"));
            return;
        }

        detach(segment);
        try
        {
            segment.writer.close();
            long size = Files.size(segment.videoPath);
            if (size <= 0)
                throw new RecordingSpoolException("Finalized recording is empty: " + segment.videoPath);

            Instant endTime = segment.startTime.plusNanos(endCaptureNanos - segment.startCaptureNanos);
            completed.add(new RecordingSpoolEntry(
                    segment.segmentId, deviceId, segment.videoPath, segment.startTime, endTime,
                    size, 0, RecordingSpoolStatus.ACTIVE, Instant.now(), null));
        }
        catch (IOException | RuntimeException failure)
        {
            quarantinePartial(segment.videoPath, failure);
            LOG.log(System.Logger.Level.WARNING, "Could not finalize recording segment: " + segment.videoPath,
                    failure);
        }
    }

    public synchronized List<RecordingSpoolEntry> finishRemainingSegments(List<AudioSampleBuffer> audioChunks)
    {
        if (closed || finished)
            return List.of();

        Objects.requireNonNull(audioChunks, "audioChunks cannot be null");
        finished = true;
        List<RecordingSpoolEntry> completed = new ArrayList<>(2);
        try
        {
            enqueueAudio(audioChunks);
            finishClosingIfReady(false, true, completed);

            SegmentState segment = activeSegment;
            if (segment != null)
            {
                long durationNanos = Math.max(1L,
                        Math.round((segment.lastVideoTick + 1.0) * NANOS_PER_SECOND / configuredFPS));
                long endCaptureNanos = segment.startCaptureNanos + durationNanos;
                if (segment.lastVideoTick >= 0)
                    consumeAudioBefore(segment, endCaptureNanos);
                if (activeSegment == segment)
                    finalizeSegment(segment, endCaptureNanos, completed);
            }
            return List.copyOf(completed);
        }
        finally
        {
            pendingAudio.clear();
        }
    }

    @Override
    public synchronized void close()
    {
        if (closed)
            return;

        closed = true;
        try
        {
            if (closingSegment != null)
                abandonSegment(closingSegment, new RecordingSpoolException("Encoder closed before segment finish"));
        }
        finally
        {
            try
            {
                if (activeSegment != null)
                    abandonSegment(activeSegment, new RecordingSpoolException("Encoder closed before segment finish"));
            }
            finally
            {
                pendingAudio.clear();
            }
        }
    }

    private void detach(SegmentState segment)
    {
        if (activeSegment == segment)
            activeSegment = null;
        if (closingSegment == segment)
            closingSegment = null;
    }

    private void abandonSegment(SegmentState segment, RuntimeException failure)
    {
        detach(segment);
        try
        {
            segment.writer.close();
        }
        catch (RuntimeException cleanupFailure)
        {
            if (cleanupFailure != failure)
                failure.addSuppressed(cleanupFailure);
        }
        quarantinePartial(segment.videoPath, failure);
        LOG.log(System.Logger.Level.WARNING, "Abandoned recording segment: " + segment.videoPath, failure);
    }

    private boolean quarantinePartial(Path path, Exception failure)
    {
        try
        {
            // A missing file also means the original path can safely be reused.
            spoolService.quarantineActiveFile(path);
            return true;
        }
        catch (RuntimeException quarantineFailure)
        {
            if (quarantineFailure != failure)
                failure.addSuppressed(quarantineFailure);
            return false;
        }
    }

    private static void validateFrame(Frame frame)
    {
        if (frame == null || frame.isEmpty())
            throw new IllegalArgumentException("Frame cannot be null or empty");
        if (frame.getTimestamp() == null)
            throw new IllegalArgumentException("Frame timestamp cannot be null");
        if (frame.getWidth() <= 0 || frame.getHeight() <= 0)
            throw new IllegalArgumentException("Frame dimensions must be positive");
    }

    private static final class SegmentState
    {
        private final UUID segmentId;
        private final Path videoPath;
        private final Instant startTime;
        private final long startCaptureNanos;
        private final Mp4MediaWriter writer;
        private final boolean audioEnabled;
        private long lastVideoTick = -1;

        private SegmentState(UUID segmentId, Path videoPath, Instant startTime, long startCaptureNanos,
                Mp4MediaWriter writer, boolean audioEnabled)
        {
            this.segmentId = segmentId;
            this.videoPath = videoPath;
            this.startTime = startTime;
            this.startCaptureNanos = startCaptureNanos;
            this.writer = writer;
            this.audioEnabled = audioEnabled;
        }
    }
}
