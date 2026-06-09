package com.sentinelmesh.edge.util;

public class ShutdownHook 
{
	private final String threadName;
	private final Runnable shutdownAction;
	private boolean registered;
	
	public ShutdownHook(String threadName, Runnable shutdownAction)
	{
		this.threadName=threadName;
		this.shutdownAction=shutdownAction;
	}
	
	public void register()
	{
		if(registered)
			return;
		
		Thread thread=new Thread(shutdownAction, threadName);
		Runtime.getRuntime().addShutdownHook(thread);
		registered=true;
	}
	
	public boolean isRegistered()
	{
		return registered;
	}
}
