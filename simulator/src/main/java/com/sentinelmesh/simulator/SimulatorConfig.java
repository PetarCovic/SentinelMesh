package com.sentinelmesh.simulator;

public record SimulatorConfig(
		String baseUrl,
		int deviceCount,
		int durationSeconds,
		int heartbeatIntervalSeconds,
		int eventIntervalSeconds
		)
{
	public static SimulatorConfig fromArgs(String[] args)
	{
		String baseUrl="http://localhost:8080";
		int deviceCount=5;
		int durationSeconds=60;
		int heartbeatIntervalSeconds=10;
		int eventIntervalSeconds=5;
		
		for(int i=0; i<args.length; i++)
		{
			switch(args[i])
			{
				case "--base-url" -> 
				{
					requireValue(args, i);
					baseUrl=args[++i];
				}
				case "--devices" ->
				{
					requireValue(args, i);
					deviceCount=Integer.parseInt(args[++i]);
				}
				case "--duration-seconds" ->
				{
					requireValue(args, i);
					durationSeconds=Integer.parseInt(args[++i]);
				}
				case "--heartbeat-interval-seconds" ->
				{
					requireValue(args, i);
					heartbeatIntervalSeconds=Integer.parseInt(args[++i]);
				}
				case "--event-interval-seconds" ->
				{
					requireValue(args, i);
					eventIntervalSeconds=Integer.parseInt(args[++i]);
				}
				default -> throw new IllegalArgumentException("Unknown argument: "+args[i]);
			}
		}
		
		if(deviceCount<=0)
			throw new IllegalArgumentException("--devices must be greater than 0");
		
		if(durationSeconds<=0)
			throw new IllegalArgumentException("--duration-seconds must be greater than 0");
		
		if(heartbeatIntervalSeconds<=0)
			throw new IllegalArgumentException("--heartbeat-interval-seconds must be greater than 0");
		
		if(eventIntervalSeconds<=0)
			throw new IllegalArgumentException("--event-interval-seconds must be greater than 0");
		
		return new SimulatorConfig(
				baseUrl,
				deviceCount,
				durationSeconds,
				heartbeatIntervalSeconds,
				eventIntervalSeconds
				);
	}
	
	private static void requireValue(String[] args, int index)
	{
		if(index+1>=args.length)
			throw new IllegalArgumentException(args[index]+" requires a value");
	}
}