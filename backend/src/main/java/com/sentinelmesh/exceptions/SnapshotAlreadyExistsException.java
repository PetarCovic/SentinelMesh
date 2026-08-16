package com.sentinelmesh.exceptions;

import java.util.UUID;

public class SnapshotAlreadyExistsException extends RuntimeException
{
	public SnapshotAlreadyExistsException(UUID id)
	{
		super("Snapshot already exists with id: "+id);
	}
}
