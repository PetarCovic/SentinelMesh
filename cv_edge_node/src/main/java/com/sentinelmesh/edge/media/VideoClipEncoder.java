package com.sentinelmesh.edge.media;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.OpenCVFrameConverter;

import com.sentinelmesh.edge.audio.AudioRange;
import com.sentinelmesh.edge.audio.AudioSampleBuffer;
import com.sentinelmesh.edge.camera.Frame;
import com.sentinelmesh.edge.timing.MediaTiming;

public class VideoClipEncoder
{
    private final double fps;
    private final double recordingAudioGainDb;

    public VideoClipEncoder(double fps, double recordingAudioGainDb)
    {
        validateFps(fps);
        validateRecordingAudioGainDb(recordingAudioGainDb);
        
        this.fps = fps;
        this.recordingAudioGainDb=recordingAudioGainDb;
    }

    public byte[] encodeMp4(List<Frame> frames)
    {
        return encodeMp4(frames, fps);
    }

    // Keep the original behavior: write each input frame at the requested FPS.
    public byte[] encodeMp4(List<Frame> frames, double encodingFPS)
    {
        validateFrames(frames);
        validateFps(encodingFPS);
        Frame firstFrame = frames.get(0);

        return encodeTemporaryMp4(output ->
        {
            try (OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
                 FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(
                         output.toFile(), firstFrame.getWidth(), firstFrame.getHeight()))
            {
                recorder.setFormat("mp4");
                recorder.setFrameRate(encodingFPS);
                recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
                recorder.start();

                for (Frame frame : frames)
                    recorder.record(converter.convert(frame.getImage()));
            }
        });
    }

    // Both lists are in capture order. An empty audio list makes a video-only clip.
    public byte[] encodeMp4(List<Frame> frames, List<AudioRange> audioRanges)
    {
        validateFrames(frames);
        validateAudioRanges(audioRanges);

        try
        {
            return encodeAttempt(frames, audioRanges);
        }
        catch (RuntimeException firstFailure)
        {
            if (audioRanges.isEmpty())
                throw firstFailure;

            byte[] videoOnly;
            try
            {
                videoOnly = encodeAttempt(frames, List.of());
            }
            catch (RuntimeException retryFailure)
            {
                if (retryFailure != firstFailure)
                    retryFailure.addSuppressed(firstFailure);
                throw retryFailure;
            }

            System.err.println("Audio-enabled clip encoding failed; returning video only: "
                    + firstFailure.getMessage());
            return videoOnly;
        }
    }

    private byte[] encodeAttempt(List<Frame> frames, List<AudioRange> audioRanges)
    {
        Frame firstFrame = frames.get(0);
        long originNanos = firstFrame.getCaptureNanos();
        boolean audioEnabled = !audioRanges.isEmpty();
        int sampleRate = audioEnabled ? audioRanges.get(0).chunk().getSampleRate() : 0;
        int channels = audioEnabled ? audioRanges.get(0).chunk().getChannels() : 0;

        return encodeTemporaryMp4(output ->
        {
            try (Mp4MediaWriter writer = new Mp4MediaWriter(
                    output, firstFrame.getWidth(), firstFrame.getHeight(), fps,
                    originNanos, sampleRate, channels, audioEnabled, recordingAudioGainDb))
            {
                int videoIndex = 0;
                int audioIndex = 0;
                boolean wroteVideo = false;

                while (videoIndex < frames.size() || audioIndex < audioRanges.size())
                {
                    Frame frame = videoIndex < frames.size() ? frames.get(videoIndex) : null;
                    AudioRange range = audioIndex < audioRanges.size() ? audioRanges.get(audioIndex) : null;

                    if (frame != null && (range == null || frame.getCaptureNanos() - audioStartNanos(range) <= 0))
                    {
                        if (writer.writeVideoFrame(frame))
                            wroteVideo = true;
                        videoIndex++;
                    }
                    else
                    {
                        writer.writeAudioFrames(range.chunk(), range.offsetFrames(), range.frameCount());
                        audioIndex++;
                    }
                }

                if (!wroteVideo)
                    throw new IllegalStateException("No video frames were written to the clip");
            }
        });
    }

    private static long audioStartNanos(AudioRange range)
    {
        AudioSampleBuffer chunk = range.chunk();
        return chunk.getCaptureStartNanos()
                + MediaTiming.framesToNanos(range.offsetFrames(), chunk.getSampleRate());
    }

    private static void validateFps(double fps)
    {
        if (!Double.isFinite(fps) || fps <= 0)
            throw new IllegalArgumentException("FPS must be finite and greater than 0");
    }
    
    private static void validateRecordingAudioGainDb(double recordingAudioGainDb)
    {
    	if(!Double.isFinite(recordingAudioGainDb)
				|| (recordingAudioGainDb<-24 || recordingAudioGainDb>24))
			throw new IllegalArgumentException("recordingAudioGainDb must between -24 and 24 Db");
    }

    private static void validateFrames(List<Frame> frames)
    {
        if (frames == null || frames.isEmpty())
            throw new IllegalArgumentException("Frames cannot be null or empty");

        Frame firstFrame = frames.get(0);
        if (firstFrame == null || firstFrame.isEmpty())
            throw new IllegalArgumentException("Frame cannot be null or empty");

        int width = firstFrame.getWidth();
        int height = firstFrame.getHeight();
        if (width <= 0 || height <= 0)
            throw new IllegalArgumentException("Frame dimensions must be positive");

        for (Frame frame : frames)
        {
            if (frame == null || frame.isEmpty())
                throw new IllegalArgumentException("Frame cannot be null or empty");
            if (frame.getWidth() != width || frame.getHeight() != height)
                throw new IllegalArgumentException("All frames must have the same dimensions");
        }
    }

    private static void validateAudioRanges(List<AudioRange> audioRanges)
    {
        if (audioRanges == null)
            throw new IllegalArgumentException("audioRanges cannot be null");

        AudioSampleBuffer firstChunk = null;
        for (AudioRange range : audioRanges)
        {
            if (range == null)
                throw new IllegalArgumentException("Audio range cannot be null");

            AudioSampleBuffer chunk = range.chunk();
            if (firstChunk == null)
                firstChunk = chunk;
            else if (chunk.getSampleRate() != firstChunk.getSampleRate()
                    || chunk.getChannels() != firstChunk.getChannels())
                throw new IllegalArgumentException("All audio ranges must have the same sample rate and channels");
        }
    }

    private static byte[] encodeTemporaryMp4(ClipFileWriter fileWriter)
    {
        Path tmpFile = null;
        try
        {
            tmpFile = Files.createTempFile("sentinelmesh-video-", ".mp4");
            fileWriter.write(tmpFile); // The recorder must be closed before reading the MP4.
            return Files.readAllBytes(tmpFile);
        }
        catch (IOException ex)
        {
            throw new IllegalStateException("Failed to encode MP4 video clip", ex);
        }
        finally
        {
            if (tmpFile != null)
            {
                try
                {
                    Files.deleteIfExists(tmpFile);
                }
                catch (IOException | SecurityException ex)
                {
                    System.err.println("Failed to delete temporary video clip file "
                            + tmpFile + ": " + ex.getMessage());
                }
            }
        }
    }

    @FunctionalInterface
    private interface ClipFileWriter
    {
        void write(Path output) throws IOException;
    }
}
