package com.sentinelmesh.exceptions;

import java.util.UUID;

public class SecurityEventNotFoundException extends RuntimeException
{
	public SecurityEventNotFoundException(UUID id)
	{
		super("Security event not found with id: "+id);
	}
}
