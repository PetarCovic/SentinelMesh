package com.sentinelmesh.async;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EventProcessingPropertiesTest {

    @Test
    void defaultValues_shouldBeCorrect() {
        EventProcessingProperties properties = new EventProcessingProperties();

        assertEquals("sentinelmesh:event-processing", properties.getQueueName());
        assertTrue(properties.isEnabled());
    }

    @Test
    void setters_shouldUpdateValues() {
        EventProcessingProperties properties = new EventProcessingProperties();

        properties.setQueueName("custom:event-queue");
        properties.disable();

        assertEquals("custom:event-queue", properties.getQueueName());
        assertFalse(properties.isEnabled());
    }
}