package com.sentinelmesh.edge.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JsonUtils 
{
	private final ObjectMapper objectMapper;
	
	public JsonUtils()
	{
		this.objectMapper = new ObjectMapper();
		this.objectMapper.registerModule(new JavaTimeModule());
		this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	}
	
	public String toJson(Object value)
	{
		if(value == null)
			throw new IllegalArgumentException("Value cannot be null");
		
		try
		{
			return objectMapper.writeValueAsString(value);
		}
		catch(JsonProcessingException ex)
		{
			throw new IllegalStateException("Failed to serialize object to JSON", ex);
		}
	}
	
	public <T> T fromJson(String json, Class<T> type)
	{
		if(json == null || json.isBlank())
			throw new IllegalArgumentException("JSON cannot be null or blank");
		
		if(type == null)
			throw new IllegalArgumentException("Type cannot be null");
		
		try
		{
			return objectMapper.readValue(json, type);
		}
		catch(JsonProcessingException ex)
		{
			throw new IllegalStateException("Failed to deserialize JSON into " + type.getSimpleName(), ex);
		}
	}
}