package com.sentinelmesh.async;

import java.util.Optional;
import java.util.UUID;

public interface EventProcessingQueue 
{
	void enqueue(UUID securityEventId);
	
	Optional<UUID> dequeue();
}
