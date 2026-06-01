package com.sentinelmesh;

import com.sentinelmesh.async.EventProcessingQueue;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@TestConfiguration
public class TestQueueConfig {

    @Bean
    @Primary
    public InMemoryEventProcessingQueue inMemoryEventProcessingQueue() {
        return new InMemoryEventProcessingQueue();
    }

    public static class InMemoryEventProcessingQueue implements EventProcessingQueue {

        private final List<UUID> queuedEventIds = new ArrayList<>();

        @Override
        public void enqueue(UUID securityEventId) {
            queuedEventIds.add(securityEventId);
        }

        @Override
        public Optional<UUID> dequeue() {
            if (queuedEventIds.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(queuedEventIds.remove(0));
        }

        public List<UUID> getQueuedEventIds() {
            return queuedEventIds;
        }

        public void clear() {
            queuedEventIds.clear();
        }
    }
}