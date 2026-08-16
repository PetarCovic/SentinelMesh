package com.sentinelmesh.edge.media;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.OpenCVFrameConverter;

import com.sentinelmesh.edge.camera.Frame;

public class VideoClipEncoder 
{
	private final double fps;
	
	public VideoClipEncoder(double fps)
	{
		if(fps <= 0
		        || Double.isNaN(fps)
		        || Double.isInfinite(fps))
		{
		    throw new IllegalArgumentException(
		            "FPS must be finite and greater than 0"
		    );
		}
		
		this.fps=fps;
	}
	
	public byte[] encodeMp4(List<Frame> frames)
	{
		return encodeMp4(frames, fps);
	}
	
	public byte[] encodeMp4(List<Frame> frames, double encodingFPS)
	{
		validateFrames(frames);
		
		if(encodingFPS <= 0
		        || Double.isNaN(encodingFPS)
		        || Double.isInfinite(encodingFPS))
		{
		    throw new IllegalArgumentException(
		            "EncodingFPS must be finite and greater than 0"
		    );
		}
		
		int width=getWidth(frames.get(0));
		int height=getHeight(frames.get(0));
		
		if(width <= 0 || height <= 0)
			throw new IllegalArgumentException("Frame width and height must be greater than 0");
		
		Path tmpFile = null;
		boolean started=false;
		try {
			tmpFile = Files.createTempFile("sentinelmesh-video-", ".mp4");
			
			OpenCVFrameConverter.ToMat converter=new OpenCVFrameConverter.ToMat();
			
			FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(tmpFile.toFile(), width, height);
			recorder.setFormat("mp4");
			recorder.setFrameRate(encodingFPS);
			recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
			
			try
			{
				recorder.start();
				started=true;
			
				for(Frame frame : frames)
				{
					recorder.record(converter.convert(frame.getImage()));
				}
			}
			finally
			{
				if(started)
				{
					try {
						recorder.stop();
					} finally {
						recorder.close();
					}
				}
				else
					recorder.close();
				
				converter.close();
			}
			
			byte[] encodedFile=Files.readAllBytes(tmpFile);
			return encodedFile;
		} catch (IOException e)
		{
			throw new IllegalStateException("Failed to encode MP4 video clip", e);
		}
		finally
		{
			if(tmpFile != null)
			{
			    try {
					Files.deleteIfExists(tmpFile);
				} catch (IOException e) 
			    {
					System.out.println("Failed to delete temporary video clip file: " + tmpFile);
				}
			}
		}
	}
	
	private void validateFrames(List<Frame> frames)
	{
		if(frames==null)
			throw new IllegalArgumentException("Frames cannot be null");
		
		if(frames.isEmpty())
			throw new IllegalArgumentException("Frames cannot be empty");
		
		for(Frame frame : frames)
			if(frame==null || frame.isEmpty())
				throw new IllegalArgumentException("Frame cannot be null or empty");
	}
	
	private int getWidth(Frame frame)
	{
		return frame.getWidth();
	}
	
	private int getHeight(Frame frame)
	{
		return frame.getHeight();
	}
}
