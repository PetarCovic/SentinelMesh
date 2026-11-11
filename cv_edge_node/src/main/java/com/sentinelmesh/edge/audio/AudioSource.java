package com.sentinelmesh.edge.audio;

public interface AudioSource extends AutoCloseable
{
	public boolean start();
	
	public int read(byte[] buffer, int offset, int length);
	
	public boolean isAvailable();
	
	public void stop();
	
	public void close();
}
