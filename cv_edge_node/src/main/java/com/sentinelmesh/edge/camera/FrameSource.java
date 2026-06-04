package com.sentinelmesh.edge.camera;

public interface FrameSource
{
	boolean open();
	
	boolean read(Frame frame);
	
	boolean isOpen();
	
	void close();
	
	String getSourceName();
}
