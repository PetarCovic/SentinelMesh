package com.sentinelmesh.simulator;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeviceRegistrationClient 
{
	 private static final Pattern ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");
	 private static final Pattern API_KEY_PATTERN = Pattern.compile("\"apiKey\"\\s*:\\s*\"([^\"]+)\"");
	 
	 private final String baseUrl;
	 private final HttpJsonClient httpJsonClient;
	 
	 public DeviceRegistrationClient(String baseUrl, HttpJsonClient httpJsonClient)
	 {
		 this.baseUrl=baseUrl;
		 this.httpJsonClient=httpJsonClient;
	 }
	 
	 public SimulatedDevice registerDevice(int index) throws IOException, InterruptedException
	 {
		 String name="Simulated Camera "+index;
		 String type="CAMERA";
		 String location="Zone "+index;
		 
		 String json = String.format("""
	                {
	                  "name": "%s",
	                  "type": "%s",
	                  "location": "%s"
	                }
	                """, name, type, location);
		 
		 String response=httpJsonClient.postJson(baseUrl+"/api/devices", json);
		 
		 String id = JsonFieldExtractor.extractStringField(response, "id");
		 String apiKey = JsonFieldExtractor.extractStringField(response, "apiKey");
		 
		 return new SimulatedDevice(id, apiKey, name, type, location);
	 }
}
