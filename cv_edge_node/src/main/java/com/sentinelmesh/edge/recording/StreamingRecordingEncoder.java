package com.sentinelmesh.edge.recording;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.OpenCVFrameConverter;

import com.sentinelmesh.edge.camera.Frame;
import com.sentinelmesh.edge.exceptions.RecordingSpoolException;

public class StreamingRecordingEncoder implements AutoCloseable
{
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMAT =
            DateTimeFormatter
                    .ofPattern("yyyyMMdd'T'HHmmssSSS'Z'")
                    .withZone(ZoneOffset.UTC);

    private final RecordingSpoolDirectories spoolDirectories;
    private final UUID deviceId;
    private final int segmentDurationSeconds;
    private final double configuredFPS;

    private FFmpegFrameRecorder recorder;
    private OpenCVFrameConverter.ToMat converter;

    private UUID currentSegmentId;
    private Path currentVideoPath;
    private Instant segmentStartTime;
    private Instant lastFrameTime;

    private int frameWidth;
    private int frameHeight;
    private int frameCount;

    private boolean recorderStarted;
    private boolean closed;

    public StreamingRecordingEncoder(
            RecordingSpoolDirectories spoolDirectories,
            UUID deviceId,
            int segmentDurationSeconds,
            double configuredFPS
    )
    {
        if(spoolDirectories == null)
        {
            throw new IllegalArgumentException(
                    "SpoolDirectories cannot be null"
            );
        }

        if(deviceId == null)
        {
            throw new IllegalArgumentException(
                    "DeviceId cannot be null"
            );
        }

        if(segmentDurationSeconds <= 0)
        {
            throw new IllegalArgumentException(
                    "SegmentDurationSeconds must be greater than 0"
            );
        }

        if(configuredFPS <= 0
                || Double.isNaN(configuredFPS)
                || Double.isInfinite(configuredFPS))
        {
            throw new IllegalArgumentException(
                    "ConfiguredFPS must be finite and greater than 0"
            );
        }

        this.spoolDirectories = spoolDirectories;
        this.deviceId = deviceId;
        this.segmentDurationSeconds = segmentDurationSeconds;
        this.configuredFPS = configuredFPS;

        this.closed = false;
        this.recorderStarted = false;
    }

    public synchronized Optional<RecordingSpoolEntry> acceptFrame(
            Frame frame
    )
    {
        ensureOpen();
        validateFrame(frame);

        Instant frameTimestamp = frame.getTimestamp();

        if(lastFrameTime != null
                && frameTimestamp.isBefore(lastFrameTime))
        {
            throw new IllegalArgumentException(
                    "Frame timestamp cannot be before the previous frame"
            );
        }

        Optional<RecordingSpoolEntry> completedEntry =
                Optional.empty();

        if(recorder == null)
        {
            openSegment(frame);
        }
        else if(shouldRotate(frameTimestamp))
        {
            completedEntry = finalizeCurrentSegment();
            openSegment(frame);
        }
        else
        {
            validateFrameDimensions(frame);
        }

        writeFrame(frame);

        return completedEntry;
    }

    public synchronized Optional<RecordingSpoolEntry>
            finalizeCurrentSegment()
    {
        if(recorder == null)
        {
            return Optional.empty();
        }

        UUID completedSegmentId = currentSegmentId;
        Path completedVideoPath = currentVideoPath;
        Instant completedStartTime = segmentStartTime;
        Instant completedEndTime = lastFrameTime;
        int completedFrameCount = frameCount;

        Exception cleanupFailure = closeCurrentResources();

        resetCurrentSegmentState();

        if(cleanupFailure != null)
        {
            throw new RecordingSpoolException(
                    "Failed to finalize recording segment: "
                            + completedVideoPath,
                    cleanupFailure
            );
        }

        if(completedFrameCount <= 0
                || completedEndTime == null)
        {
            throw new RecordingSpoolException(
                    "Recording segment contained no completed frames: "
                            + completedVideoPath
            );
        }

        /*
         * RecordingSpoolEntry requires end time to be after start time.
         * A one-frame partial segment has only one observed timestamp, so
         * estimate its end using one configured frame interval.
         */
        if(!completedEndTime.isAfter(completedStartTime))
        {
            long frameDurationNanos =
                    Math.max(
                            1L,
                            Math.round(
                                    1_000_000_000.0 / configuredFPS
                            )
                    );

            completedEndTime =
                    completedStartTime.plusNanos(frameDurationNanos);
        }

        try
        {
            long fileSizeBytes =
                    Files.size(completedVideoPath);

            RecordingSpoolEntry completedEntry =
                    new RecordingSpoolEntry(
                            completedSegmentId,
                            deviceId,
                            completedVideoPath,
                            completedStartTime,
                            completedEndTime,
                            fileSizeBytes,
                            0,
                            RecordingSpoolStatus.ACTIVE,
                            Instant.now(),
                            null
                    );

            return Optional.of(completedEntry);
        }
        catch(IOException ex)
        {
            throw new RecordingSpoolException(
                    "Failed to inspect completed recording segment: "
                            + completedVideoPath,
                    ex
            );
        }
    }

    private void openSegment(Frame firstFrame)
    {
        currentSegmentId = UUID.randomUUID();
        segmentStartTime = firstFrame.getTimestamp();
        lastFrameTime = null;
        frameCount = 0;

        frameWidth = firstFrame.getWidth();
        frameHeight = firstFrame.getHeight();

        String formattedStartTime =
                FILE_TIMESTAMP_FORMAT.format(segmentStartTime);

        String filename =
                formattedStartTime
                + "_"
                + deviceId
                + "_"
                + currentSegmentId
                + ".mp4";

        Path activeDirectory =
                spoolDirectories
                        .getActive()
                        .toAbsolutePath()
                        .normalize();

        currentVideoPath =
                activeDirectory
                        .resolve(filename)
                        .normalize();

        if(!currentVideoPath.startsWith(activeDirectory))
        {
            resetCurrentSegmentState();

            throw new RecordingSpoolException(
                    "Recording path escapes the active spool directory"
            );
        }

        try
        {
            converter =
                    new OpenCVFrameConverter.ToMat();

            recorder =
                    new FFmpegFrameRecorder(
                            currentVideoPath.toFile(),
                            frameWidth,
                            frameHeight
                    );

            recorder.setFormat("mp4");
            recorder.setFrameRate(configuredFPS);
            recorder.setVideoCodec(
                    avcodec.AV_CODEC_ID_H264
            );

            recorder.start();
            recorderStarted = true;
        }
        catch(Exception ex)
        {
            Exception cleanupFailure =
                    closeCurrentResources();

            Path failedPath = currentVideoPath;

            resetCurrentSegmentState();

            RecordingSpoolException spoolException =
                    new RecordingSpoolException(
                            "Failed to open streaming recording segment: "
                                    + failedPath,
                            ex
                    );

            if(cleanupFailure != null)
            {
                spoolException.addSuppressed(cleanupFailure);
            }

            throw spoolException;
        }
    }

    private void writeFrame(Frame frame)
    {
        validateFrameDimensions(frame);

        try
        {
            recorder.record(
                    converter.convert(frame.getImage())
            );

            lastFrameTime = frame.getTimestamp();
            frameCount++;
        }
        catch(Exception ex)
        {
            Path failedPath = currentVideoPath;

            Exception cleanupFailure =
                    closeCurrentResources();

            resetCurrentSegmentState();

            RecordingSpoolException spoolException =
                    new RecordingSpoolException(
                            "Failed to write frame to recording segment: "
                                    + failedPath,
                            ex
                    );

            if(cleanupFailure != null)
            {
                spoolException.addSuppressed(cleanupFailure);
            }

            throw spoolException;
        }
    }

    private boolean shouldRotate(Instant frameTimestamp)
    {
        Instant rotationTime =
                segmentStartTime.plusSeconds(
                        segmentDurationSeconds
                );

        return !frameTimestamp.isBefore(rotationTime);
    }

    private void validateFrame(Frame frame)
    {
        if(frame == null || frame.isEmpty())
        {
            throw new IllegalArgumentException(
                    "Frame cannot be null or empty"
            );
        }

        if(frame.getTimestamp() == null)
        {
            throw new IllegalArgumentException(
                    "Frame timestamp cannot be null"
            );
        }

        if(frame.getWidth() <= 0
                || frame.getHeight() <= 0)
        {
            throw new IllegalArgumentException(
                    "Frame dimensions must be greater than 0"
            );
        }
    }

    private void validateFrameDimensions(Frame frame)
    {
        if(frame.getWidth() != frameWidth
                || frame.getHeight() != frameHeight)
        {
            throw new RecordingSpoolException(
                    "Frame dimensions changed during an active "
                    + "recording segment"
            );
        }
    }

    private Exception closeCurrentResources()
    {
        Exception failure = null;

        if(recorder != null)
        {
            if(recorderStarted)
            {
                try
                {
                    recorder.stop();
                }
                catch(Exception ex)
                {
                    failure = ex;
                }
            }

            try
            {
                recorder.close();
            }
            catch(Exception ex)
            {
                if(failure == null)
                {
                    failure = ex;
                }
                else
                {
                    failure.addSuppressed(ex);
                }
            }
        }

        if(converter != null)
        {
            try
            {
                converter.close();
            }
            catch(Exception ex)
            {
                if(failure == null)
                {
                    failure = ex;
                }
                else
                {
                    failure.addSuppressed(ex);
                }
            }
        }

        return failure;
    }

    private void resetCurrentSegmentState()
    {
        recorder = null;
        converter = null;

        currentSegmentId = null;
        currentVideoPath = null;
        segmentStartTime = null;
        lastFrameTime = null;

        frameWidth = 0;
        frameHeight = 0;
        frameCount = 0;

        recorderStarted = false;
    }

    private void ensureOpen()
    {
        if(closed)
        {
            throw new IllegalStateException(
                    "StreamingRecordingEncoder is closed"
            );
        }
    }

    @Override
    public synchronized void close()
    {
        if(closed)
        {
            return;
        }

        try
        {
            finalizeCurrentSegment();
        }
        finally
        {
            closed = true;
        }
    }
}