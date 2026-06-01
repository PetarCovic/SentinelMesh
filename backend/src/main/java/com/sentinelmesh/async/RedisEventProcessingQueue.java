package com.sentinelmesh.async;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisEventProcessingQueue implements EventProcessingQueue
{
	private final StringRedisTemplate redisTemplate;
	private final EventProcessingProperties properties;
	
	public RedisEventProcessingQueue(
			StringRedisTemplate redisTemplate,
			EventProcessingProperties properties
			)
	{
		this.redisTemplate=redisTemplate;
		this.properties=properties;
	}
	
	@Override
	public void enqueue(UUID securityEventId)
	{
		redisTemplate.opsForList()
				.leftPush(properties.getQueueName(), securityEventId.toString());
	}
	
	@Override
	public Optional<UUID> dequeue()
	{
		String value=redisTemplate.opsForList()
				.rightPop(properties.getQueueName(), Duration.ofSeconds(2));
		
		if(value==null)
			return Optional.empty();
		
		return Optional.of(UUID.fromString(value));
	}
}
