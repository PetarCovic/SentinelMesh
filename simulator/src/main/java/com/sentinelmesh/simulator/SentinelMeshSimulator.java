package com.sentinelmesh.simulator;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SentinelMeshSimulator 
{
	public static void main(String[] args) throws Exception
	{
		SimulatorConfig config=SimulatorConfig.fromArgs(args);
		
        System.out.println("SentinelMesh Simulator starting...");
        System.out.println("Base URL: " + config.baseUrl());
        System.out.println("Devices: " + config.deviceCount());
        System.out.println("Duration seconds: " + config.durationSeconds());
        System.out.println("Heartbeat interval seconds: " + config.heartbeatIntervalSeconds());
        System.out.println("Event interval seconds: " + config.eventIntervalSeconds());
        System.out.println();
        
        HttpJsonClient httpJsonClient=new HttpJsonClient();
        
        DeviceRegistrationClient registrationClient=
        		new DeviceRegistrationClient(config.baseUrl(), httpJsonClient);
        
        HeartbeatClient heartbeatClient=new HeartbeatClient(config.baseUrl(), httpJsonClient);
        
        EventClient eventClient=new EventClient(config.baseUrl(), httpJsonClient);
        
        List<SimulatedDevice> devices=registerDevices(config.deviceCount(), registrationClient);
        
        runSimulation(config, devices, heartbeatClient, eventClient);
        
        System.out.println();
        System.out.println("SentinelMesh Simulator Finished");
	}
	
	private static List<SimulatedDevice> registerDevices(
			int deviceCount,
			DeviceRegistrationClient registrationClient
			) throws Exception
	{
		List<SimulatedDevice> devices=new ArrayList<>();
		
		for(int i=1; i<=deviceCount; i++)
		{
			SimulatedDevice device=registrationClient.registerDevice(i);
			devices.add(device);
			
			System.out.println("Registered device:");
            System.out.println("  name: " + device.name());
            System.out.println("  id: " + device.id());
            System.out.println("  location: " + device.location());
		}
		
		System.out.println();
		
		return devices;
	}
	
	private static void runSimulation(
			SimulatorConfig config,
			List<SimulatedDevice> devices,
			HeartbeatClient heartbeatClient,
			EventClient eventClient
			) throws Exception
	{
		Instant startedAt=Instant.now();
		Instant endsAt=startedAt.plusSeconds(config.durationSeconds());
		
		Instant nextHeartbeatAt=startedAt;
		Instant nextEventAt=startedAt;
		
		int heartbeatCount=0;
		int eventCount=0;
		int alertCandidateCount=0;
		
		while(Instant.now().isBefore(endsAt))
		{
			Instant now=Instant.now();
			
			if(!now.isBefore(nextHeartbeatAt))
			{
				for(SimulatedDevice device : devices)
				{
					try
					{
						heartbeatClient.sendHeartbeat(device);
						heartbeatCount++;
						
						System.out.println("["+device.name()+"] heartbeat sent");
					} catch(Exception ex)
					{
						System.out.println("[" + device.name() + "] heartbeat failed: " + ex.getMessage());
					}
				}
				
				nextHeartbeatAt=now.plusSeconds(config.heartbeatIntervalSeconds());
			}
			
			if(!now.isBefore(nextEventAt))
			{
				for(SimulatedDevice device : devices)
				{
					try
					{
						EventClient.SentEvent event=eventClient.sendRandomEvent(device);
						eventCount++;
						
						if(event.severity().equals("HIGH") 
								|| event.severity().equals("CRITICAL"))
						{
							alertCandidateCount++;
						}
						
						System.out.println(
                                "[" + device.name() + "] event sent: "
                                        + event.eventType()
                                        + " / "
                                        + event.severity()
                                        + " / confidence="
                                        + String.format("%.2f", event.confidence()));
					}catch (Exception ex) 
					{
                        System.out.println("[" + device.name() + "] event failed: " + ex.getMessage());
                    }
				}
				
				nextEventAt=now.plusSeconds(config.eventIntervalSeconds());
			}
			
			Thread.sleep(250);
		}
		
		System.out.println();
        System.out.println("Simulation summary:");
        System.out.println("  Runtime: " + Duration.between(startedAt, Instant.now()).toSeconds() + " seconds");
        System.out.println("  Devices: " + devices.size());
        System.out.println("  Heartbeats sent: " + heartbeatCount);
        System.out.println("  Events sent: " + eventCount);
        System.out.println("  Alert candidate events: " + alertCandidateCount);
	}
}
