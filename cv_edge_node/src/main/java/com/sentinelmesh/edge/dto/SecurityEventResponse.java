package com.sentinelmesh.edge.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SecurityEventResponse 
{
	private UUID id;
	
	public SecurityEventResponse()
	{
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id=id;
	}
}
