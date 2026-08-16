package com.sentinelmesh.exceptions;

import java.util.UUID;

public class SnapshotNotFoundException extends RuntimeException
{
	public SnapshotNotFoundException(UUID id)
	{
		super("Snapshot not found with id: "+id);
	}
}
