package com.sentinelmesh.exceptions;

import java.util.UUID;

public class AlertNotFoundException extends RuntimeException
{
	public AlertNotFoundException(UUID id)
	{
		super("Alert not found with id: "+id);
	}
}
