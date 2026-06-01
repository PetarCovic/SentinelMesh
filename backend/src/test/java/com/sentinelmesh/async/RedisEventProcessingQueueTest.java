package com.sentinelmesh.async;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisEventProcessingQueueTest {

    private StringRedisTemplate redisTemplate;
    private ListOperations<String, String> listOperations;
    private EventProcessingProperties properties;
    private RedisEventProcessingQueue queue;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        listOperations = mock(ListOperations.class);
        properties = new EventProcessingProperties();

        when(redisTemplate.opsForList()).thenReturn(listOperations);

        queue = new RedisEventProcessingQueue(redisTemplate, properties);
    }

    @Test
    void enqueue_shouldPushEventIdToRedisQueue() {
        UUID eventId = UUID.randomUUID();

        queue.enqueue(eventId);

        verify(listOperations).leftPush(
                "sentinelmesh:event-processing",
                eventId.toString()
        );
    }

    @Test
    void enqueue_shouldUseConfiguredQueueName() {
        properties.setQueueName("custom:queue");

        UUID eventId = UUID.randomUUID();

        queue.enqueue(eventId);

        verify(listOperations).leftPush(
                "custom:queue",
                eventId.toString()
        );
    }

    @Test
    void dequeue_shouldReturnEventIdWhenRedisHasValue() {
        UUID eventId = UUID.randomUUID();

        when(listOperations.rightPop(
                eq("sentinelmesh:event-processing"),
                eq(Duration.ofSeconds(2))
        )).thenReturn(eventId.toString());

        Optional<UUID> result = queue.dequeue();

        assertTrue(result.isPresent());
        assertEquals(eventId, result.get());
    }

    @Test
    void dequeue_shouldReturnEmptyWhenRedisQueueIsEmpty() {
        when(listOperations.rightPop(
                eq("sentinelmesh:event-processing"),
                eq(Duration.ofSeconds(2))
        )).thenReturn(null);

        Optional<UUID> result = queue.dequeue();

        assertTrue(result.isEmpty());
    }

    @Test
    void dequeue_shouldUseConfiguredQueueName() {
        properties.setQueueName("custom:queue");

        UUID eventId = UUID.randomUUID();

        when(listOperations.rightPop(
                eq("custom:queue"),
                eq(Duration.ofSeconds(2))
        )).thenReturn(eventId.toString());

        Optional<UUID> result = queue.dequeue();

        assertTrue(result.isPresent());
        assertEquals(eventId, result.get());

        verify(listOperations).rightPop(
                eq("custom:queue"),
                eq(Duration.ofSeconds(2))
        );
    }
}