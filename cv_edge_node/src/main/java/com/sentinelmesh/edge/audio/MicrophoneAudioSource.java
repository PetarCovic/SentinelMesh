package com.sentinelmesh.edge.audio;

import java.util.Objects;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer.Info;
import javax.sound.sampled.TargetDataLine;

public class MicrophoneAudioSource implements AudioSource
{
	public final int SAMPLE_SIZE_IN_BITS=16;
	
	private final String deviceName;
	private final int sampleRate;
	private final int channels;
	
	private AudioFormat audioFormat;
	private volatile TargetDataLine dataLine;
	private boolean isActive;
	private boolean isClosed;
	
	public MicrophoneAudioSource(String deviceName, int sampleRate, int channels)
	{
		if(sampleRate<=0)
			throw new IllegalArgumentException("sampleRate must be positive");
		
		if(channels!=1 && channels!=2)
			throw new IllegalArgumentException("channels must be 1 or 2");
		
		if(deviceName==null || deviceName.isBlank())
			this.deviceName=null;
		else
			this.deviceName=deviceName;
		
		this.sampleRate=sampleRate;
		this.channels=channels;
		
		this.audioFormat=new AudioFormat(
				AudioFormat.Encoding.PCM_SIGNED,
				sampleRate,
				SAMPLE_SIZE_IN_BITS,
				channels,
				(SAMPLE_SIZE_IN_BITS*channels)/8,
				sampleRate, //framerate
				false
				);
		
		this.isActive=false;
		this.isClosed=false;
	}
	
	@Override
	public synchronized boolean start() 
	{
		if(isActive)
			return true;
		
		if(isClosed)
			return false;
		
		TargetDataLine dataLine = null;
		if(deviceName==null)
		{
			try {
				dataLine=AudioSystem.getTargetDataLine(audioFormat);
			} catch (LineUnavailableException
					| IllegalArgumentException
					| SecurityException e) {
				return false;
			}
		}
		else
		{
			Info[] info=AudioSystem.getMixerInfo();
			
			boolean infoFound=false;
			for(Info mixerInfo : info)
			{
				if(mixerInfo.getName().equals(deviceName))
				{
					try {
						dataLine=AudioSystem.getTargetDataLine(audioFormat, mixerInfo);
						infoFound=true;
					} catch (LineUnavailableException
							| IllegalArgumentException
							| SecurityException e) {
						return false;
					}
				}
			}
			
			if(!infoFound)
				return false;
		}
		
		try {
			dataLine.open(audioFormat);
			dataLine.start();
		} catch (LineUnavailableException
				| IllegalArgumentException
				| SecurityException e) 
		{
			dataLine.close();
			return false;
		}
		
		isActive=true;
		this.dataLine=dataLine;
		
		return true;
	}

	@Override
	public int read(byte[] buffer, int offset, int length) 
	{
		if(buffer==null)
			throw new IllegalArgumentException("buffer cannot be null");
		
		Objects.checkFromIndexSize(offset, length, buffer.length);
		if(length%audioFormat.getFrameSize()!=0)
			throw new IllegalArgumentException("Length must be a multiple of the audio frame size");
		
		TargetDataLine line=dataLine;
		
		if(line==null || !line.isOpen())
			return -1;
		
		if(line.isOpen() && length==0)
			return 0;
		
		int readBytes=line.read(buffer, offset, length);
		
		if (readBytes == 0 && (line != dataLine || !line.isOpen()))
		    return -1;

		return readBytes;
	}

	@Override
	public boolean isAvailable() 
	{
		TargetDataLine dl=dataLine;
		return dl!=null && dl.isOpen();
	}

	@Override
	public synchronized void stop()
	{
	    TargetDataLine line = dataLine;
	    dataLine = null;
	    isActive = false;

	    if (line == null)
	        return;

	    try
	    {
	        line.stop();
	    }
	    finally
	    {
	        line.close();
	    }
	}

	@Override
	public synchronized void close() 
	{
		if(isClosed)
			return;
		
		isClosed=true;
		
		stop();
	}

}
