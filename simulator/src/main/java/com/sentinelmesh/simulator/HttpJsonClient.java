package com.sentinelmesh.simulator;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class HttpJsonClient 
{
	private final HttpClient httpClient;
	
	public HttpJsonClient()
	{
		this.httpClient=HttpClient.newHttpClient();
	}
	
	public String postJson(String url, String json) throws IOException, InterruptedException
	{
		return postJson(url, json, Map.of());
	}
	
	public String postJson(
			String url,
			String json,
			Map<String, String> headers
			) throws IOException, InterruptedException
	{
		HttpRequest.Builder requestBuilder=HttpRequest.newBuilder()
				.uri(URI.create(url)).header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(json));
		
		for(Map.Entry<String, String> header : headers.entrySet())
			requestBuilder.header(header.getKey(), header.getValue());
		
		HttpResponse<String> response=httpClient.send(
				requestBuilder.build(), 
				HttpResponse.BodyHandlers.ofString());
		
		if(response.statusCode()<200 || response.statusCode()>=300)
			throw new IllegalStateException("POST "+url+" failed with status "
					+response.statusCode()+", body: "+response.body());
		
		return response.body();
	}
}
