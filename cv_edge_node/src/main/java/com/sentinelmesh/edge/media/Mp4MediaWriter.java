package com.sentinelmesh.edge.media;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.file.Path;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.OpenCVFrameConverter;

import com.sentinelmesh.edge.audio.AudioSampleBuffer;
import com.sentinelmesh.edge.camera.Frame;
import com.sentinelmesh.edge.timing.MediaTiming;

public final class Mp4MediaWriter implements AutoCloseable
{
    private static final int SILENCE_BLOCK_FRAMES = 4096;

    private final Path output;
    private final int width;
    private final int height;
    private final double fps;
    private final long originNanos;
    private final int sampleRate;
    private final int channels;
    private final FFmpegFrameRecorder recorder;
    private final OpenCVFrameConverter.ToMat converter;
    private final double audioGainMultiplier;

    private long lastVideoTick = -1;
    private long nextAudioFrame = 0;
    private boolean closed;
    private boolean failed;

    public Mp4MediaWriter(
            Path output, 
            int width,
            int height, 
            double fps,
            long originNanos, 
            int sampleRate, 
            int channels, 
            boolean audioEnabled,
            double recordingAudioGainDb
            )
    {
        if (output == null)
            throw new IllegalArgumentException("output cannot be null");

        if (width <= 0 || height <= 0)
            throw new IllegalArgumentException("width and height must be positive");

        if (!Double.isFinite(fps) || fps <= 0)
            throw new IllegalArgumentException("fps must be finite and positive");

        if (channels < 0 || channels > 2 || (audioEnabled && channels == 0))
            throw new IllegalArgumentException("Audio requires 1 or 2 channels");

        if (audioEnabled && sampleRate <= 0)
            throw new IllegalArgumentException("sampleRate must be positive");
        
        if(!Double.isFinite(recordingAudioGainDb)
				|| (recordingAudioGainDb<-24 || recordingAudioGainDb>24))
			throw new IllegalArgumentException("recordingAudioGainDb must between -24 and 24 Db");

        this.output = output;
        this.width = width;
        this.height = height;
        this.fps = fps;
        this.originNanos = originNanos;
        this.sampleRate = sampleRate;
        this.channels = audioEnabled ? channels : 0;
        this.audioGainMultiplier=Math.pow(10.0, recordingAudioGainDb/20.0);

        recorder = new FFmpegFrameRecorder(output.toFile(), width, height, this.channels);
        converter = new OpenCVFrameConverter.ToMat();

        try
        {
            recorder.setFormat("mp4");
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            recorder.setFrameRate(fps);
            recorder.setInterleaved(true);

            if (audioEnabled)
            {
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
                recorder.setSampleRate(sampleRate);
                recorder.setAudioBitrate(channels == 1 ? 64_000 : 128_000);
            }

            recorder.start();
        }
        catch (Exception ex)
        {
            try
            {
                releaseResources(false);
            }
            catch (Exception cleanupEx)
            {
                ex.addSuppressed(cleanupEx);
            }

            throw new IllegalStateException("Failed to open MP4: " + output, ex);
        }
    }

    public boolean writeVideoFrame(Frame frame)
    {
        ensureWritable();

        if (frame == null || frame.isEmpty())
            throw new IllegalArgumentException("frame cannot be null or empty");

        if (frame.getWidth() != width || frame.getHeight() != height)
            throw new IllegalArgumentException("Frame dimensions must match the MP4");

        long captureNanos = frame.getCaptureNanos();
        long tick = MediaTiming.videoTick(captureNanos, originNanos, fps);

        if (captureNanos - originNanos < 0 || tick <= lastVideoTick)
            return false;

        try
        {
            recorder.setFrameNumber(Math.toIntExact(tick));
            recorder.record(converter.convert(frame.getImage()));
            lastVideoTick = tick;
            return true;
        }
        catch (Exception ex)
        {
            throw writeFailure("video", ex);
        }
    }

    public void writeAudioFrames(AudioSampleBuffer chunk, int offsetFrames, int frameCount)
    {
        ensureWritable();

        if (channels == 0)
            throw new IllegalStateException("This MP4 has no audio track");

        if (chunk == null)
            throw new IllegalArgumentException("chunk cannot be null");

        if (chunk.getSampleRate() != sampleRate || chunk.getChannels() != channels)
            throw new IllegalArgumentException("Chunk format must match the audio track");

        int availableFrames = chunk.getFrameCount();
        if (offsetFrames < 0 || offsetFrames > availableFrames
                || frameCount < 0 || frameCount > availableFrames - offsetFrames)
            throw new IllegalArgumentException("Audio range is outside the chunk");

        if (frameCount == 0)
            return;

        long startFrame = Math.addExact(
                MediaTiming.audioFrameAt(chunk.getCaptureStartNanos(), originNanos, sampleRate),
                offsetFrames);

        if (Math.addExact(startFrame, frameCount) <= nextAudioFrame)
            return;

        if (startFrame < nextAudioFrame)
        {
            int skippedFrames = Math.toIntExact(nextAudioFrame - startFrame);
            offsetFrames += skippedFrames;
            frameCount -= skippedFrames;
            startFrame = nextAudioFrame;
        }

        int byteOffset = offsetFrames * channels * Short.BYTES;
        short[] samples = new short[frameCount * channels];
        ByteBuffer.wrap(chunk.copyPcmBytes(), byteOffset, samples.length * Short.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asShortBuffer()
                .get(samples);

        if (audioGainMultiplier != 1.0)
        {
            for (int i = 0; i < samples.length; i++)
            {
                long amplified = Math.round(samples[i] * audioGainMultiplier);
                samples[i] = (short) Math.max(Short.MIN_VALUE,
                        Math.min(Short.MAX_VALUE, amplified));
            }
        }
        
        try
        {
            writeSilence(startFrame - nextAudioFrame);
            recordPcm(ShortBuffer.wrap(samples), frameCount);
        }
        catch (Exception ex)
        {
            throw writeFailure("audio", ex);
        }
    }

    private void writeSilence(long frames) throws Exception
    {
        if (frames == 0)
            return;

        int blockSize = (int) Math.min(frames, SILENCE_BLOCK_FRAMES);
        short[] silence = new short[blockSize * channels];

        while (frames > 0)
        {
            int blockFrames = (int) Math.min(frames, blockSize);
            recordPcm(ShortBuffer.wrap(silence, 0, blockFrames * channels), blockFrames);
            frames -= blockFrames;
        }
    }

    private void recordPcm(ShortBuffer samples, int frames) throws Exception
    {
        recorder.recordSamples(sampleRate, channels, samples);
        nextAudioFrame += frames;
    }

    private void ensureWritable()
    {
        if (closed || failed)
            throw new IllegalStateException("MP4 writer is closed or failed: " + output);
    }

    private IllegalStateException writeFailure(String stream, Exception cause)
    {
        failed = true;
        return new IllegalStateException("Failed to write " + stream + " to " + output, cause);
    }

    private void releaseResources(boolean finalizeFile) throws Exception
    {
        try (converter)
        {
            if (finalizeFile)
                recorder.close();
            else
                recorder.release();
        }
    }

    @Override
    public void close()
    {
        if (closed)
            return;

        closed = true;

        try
        {
            releaseResources(!failed);
        }
        catch (Exception ex)
        {
            throw new IllegalStateException("Failed to close MP4: " + output, ex);
        }
    }
}