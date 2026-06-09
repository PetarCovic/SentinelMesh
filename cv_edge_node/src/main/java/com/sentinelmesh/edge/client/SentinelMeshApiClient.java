package com.sentinelmesh.edge.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class SentinelMeshApiClient 
{
	private final String backendBaseUrl;
	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;
	
	public SentinelMeshApiClient(String backendBaseUrl)
	{
		if(backendBaseUrl==null || backendBaseUrl.isBlank())
			throw new IllegalArgumentException("Backend base url cannot be null or blank");
		
		this.backendBaseUrl=backendBaseUrl;
		httpClient=HttpClient.newHttpClient();
		objectMapper=new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	}
	
	public String get(String path, String apiKey)
	{		
		HttpRequest request=buildRequest(path, apiKey).GET().build();

		try {
			HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
			
			int statusCode = response.statusCode();

	        if(statusCode < 200 || statusCode >= 300) {
	            throw new IllegalStateException(
	                    "GET request failed. Path: " + path +
	                    ", status: " + statusCode +
	                    ", body: " + response.body()
	            );
	        }
			
			return response.body();
		} catch (IOException e) 
		{			
			throw new IllegalStateException("GET request failed for path: "+path, e);
		} catch (InterruptedException e) 
		{
			Thread.currentThread().interrupt();
			throw new IllegalStateException("GET request interrupted for path: "+path, e);
		}
	}
	
	public String post(String path, Object body, String apiKey)
	{
		if(body==null)
			throw new IllegalArgumentException("Body cannot be null");
		
		try {
			String json = objectMapper.writeValueAsString(body);
			
			HttpRequest request=buildRequest(path, apiKey)
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(json))
					.build();
			
			HttpResponse<String> response=httpClient.send(request, BodyHandlers.ofString());
			
			int statusCode=response.statusCode();
			
			if(statusCode<200 || statusCode>=300)
				throw new IllegalStateException(
	                    "POST request failed. Path: " + path +
	                    ", status: " + statusCode +
	                    ", body: " + response.body()
	            );
			
			return response.body();
		} catch (JsonProcessingException ex) 
		{
			throw new IllegalStateException("Failed to serialize request body for path: "+path, ex);
		} catch (IOException ex) 
		{
			throw new IllegalStateException("POST request failed for path: "+path, ex);
		} catch (InterruptedException ex) 
		{
			Thread.currentThread().interrupt();
			throw new IllegalStateException("POST request failed for path: "+path, ex);
		}
	}
	
	public String patch(String path, Object body, String apiKey)
	{
		if(body==null)
			throw new IllegalArgumentException("Body cannot be null");
		
		try
		{
			String json=objectMapper.writeValueAsString(body);
			
			HttpRequest request=buildRequest(path, apiKey)
					.header("Content-Type", "application/json")
					.method("PATCH", HttpRequest.BodyPublishers.ofString(json))
					.build();
			
			HttpResponse<String> response=httpClient.send(request, BodyHandlers.ofString());
			
			int statusCode=response.statusCode();
			
			if(statusCode<200 || statusCode>=300)
				throw new IllegalStateException(
	                    "PATCH request failed. Path: " + path +
	                    ", status: " + statusCode +
	                    ", body: " + response.body()
	            );
			
			return response.body();
		} catch (JsonProcessingException ex) 
		{
			throw new IllegalStateException("Failed to serialize request body for path: "+path, ex);
		} catch (IOException ex) 
		{
			throw new IllegalStateException("PATCH request failed for path: "+path, ex);
		} catch (InterruptedException ex) 
		{
			Thread.currentThread().interrupt();
			throw new IllegalStateException("PATCH request failed for path: "+path, ex);
		}
	}
	
	public HttpRequest.Builder buildRequest(String path, String apiKey)
	{
		if(path==null || path.isBlank())
			throw new IllegalArgumentException("Path cannot be null or blank");
		
		if(apiKey==null || apiKey.isBlank())
			throw new IllegalArgumentException("Api Key cannot be null or blank");
		
		apiKey=apiKey.trim();
		
		String url=normalizeUrl(path);
		
		HttpRequest.Builder builder=HttpRequest.newBuilder()
				.uri(URI.create(url))
				.header("Accept", "application/json")
				.header("X-Device-Api-Key", apiKey);
		
		return builder;
	}
	
	public <T> T parseResponse(String responseBody, Class<T> responseType)
	{
		 if(responseBody == null || responseBody.isBlank())
		        throw new IllegalArgumentException("Response body cannot be null or blank");

		    if(responseType == null)
		        throw new IllegalArgumentException("Response type cannot be null");
		    
		    try
		    {
		    	return objectMapper.readValue(responseBody, responseType);
		    }catch(Exception ex)
		    {
		    	throw new IllegalStateException("Failed to parse backend response as "
		    			+responseType.getSimpleName(), ex);
		    }
	}
	

	
	public String normalizeUrl(String path)
	{
		path=path.trim();
		
		String baseUrl="";
		if(backendBaseUrl.endsWith("/"))
			baseUrl=backendBaseUrl.substring(0, backendBaseUrl.length()-1);
		else
			baseUrl=backendBaseUrl;
		
		String newPath="";
		if(!path.startsWith("/"))
			newPath='/'+path;
		else
			newPath=path;
		
		String url=baseUrl+newPath;
		
		return url;
	}
}