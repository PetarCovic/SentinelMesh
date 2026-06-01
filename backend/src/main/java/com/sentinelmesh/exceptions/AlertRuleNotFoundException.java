package com.sentinelmesh.exceptions;

import java.util.UUID;

public class AlertRuleNotFoundException extends RuntimeException
{
	public AlertRuleNotFoundException(UUID id)
	{
		super("Alert rule not found with id: "+id);
	}
}
