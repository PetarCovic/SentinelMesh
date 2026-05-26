package com.sentinelmesh.exceptions;

import java.util.UUID;

public class InvalidDeviceApiKeyException extends RuntimeException
{
	public InvalidDeviceApiKeyException(UUID id)
	{
		super("Invalid API key for device id: "+id);
	}
}
