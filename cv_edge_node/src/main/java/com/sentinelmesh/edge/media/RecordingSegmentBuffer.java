package com.sentinelmesh.edge.media;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;

import com.sentinelmesh.edge.camera.Frame;

@Deprecated
public class RecordingSegmentBuffer
{
    private final int maxFrames;
    private final int durationSeconds;
    private final ArrayDeque<Frame> frameDeque;

    private Instant segmentStartTime;

    public RecordingSegmentBuffer(
            int maxFrames,
            int durationSeconds)
    {
        if(maxFrames <= 0)
            throw new IllegalArgumentException(
                    "MaxFrames must be greater than 0"
            );

        if(durationSeconds <= 0)
            throw new IllegalArgumentException(
                    "DurationSeconds must be greater than 0"
            );

        this.maxFrames = maxFrames;
        this.durationSeconds = durationSeconds;
        this.frameDeque = new ArrayDeque<>();
    }

    public synchronized void add(Frame frame)
    {
        if(frame == null)
            throw new IllegalArgumentException("Frame cannot be null");
        
        if(frame.getTimestamp()==null)
        	throw new IllegalArgumentException("Frame timestamp cannot be null");

        if(frame.isEmpty())
        {
            frame.close();

            throw new IllegalArgumentException(
                    "Frame cannot be empty"
            );
        }

        if(frameDeque.size() >= maxFrames)
        {
            frame.close();

            throw new IllegalStateException(
                    "Recording segment buffer reached its safety limit"
            );
        }

        if(frameDeque.isEmpty())
            segmentStartTime = frame.getTimestamp();

        frameDeque.addLast(frame);
    }

    public synchronized boolean isSegmentReady()
    {
        if(frameDeque.isEmpty())
            return false;

        Instant segmentEndTime =
                frameDeque.getLast().getTimestamp();

        long elapsedSeconds =Duration.between(segmentStartTime, segmentEndTime).getSeconds();

        if(elapsedSeconds >= durationSeconds)
            return true;

        return frameDeque.size() >= maxFrames;
    }

    public synchronized CompletedRecordingSegment drainSegment()
    {
        if(frameDeque.isEmpty())
            throw new IllegalStateException(
                    "Cannot drain an empty recording segment buffer"
            );

        ArrayList<Frame> frames =
                new ArrayList<>(frameDeque.size());

        while(!frameDeque.isEmpty())
            frames.add(frameDeque.removeFirst());

        Instant startTime = segmentStartTime;
        Instant endTime =
                frames.get(frames.size() - 1).getTimestamp();

        segmentStartTime = null;

        return new CompletedRecordingSegment(
                frames,
                startTime,
                endTime
        );
    }

    public synchronized int size()
    {
        return frameDeque.size();
    }

    public synchronized void clear()
    {
        while(!frameDeque.isEmpty())
            frameDeque.removeFirst().close();

        segmentStartTime = null;
    }

    public synchronized void close()
    {
        clear();
    }
}