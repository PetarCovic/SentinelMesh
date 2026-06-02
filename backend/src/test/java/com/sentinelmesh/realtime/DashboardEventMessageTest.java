package com.sentinelmesh.realtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import org.junit.jupiter.api.Test;

class DashboardEventMessageTest {

    @Test
    void of_shouldCreateMessageWithTypeTimestampAndPayload() {
        DashboardEventMessage message = DashboardEventMessage.of(
                DashboardEventType.ALERT_CREATED,
                Map.of(
                        "alertId", "alert-123",
                        "severity", "HIGH"
                )
        );

        assertEquals(DashboardEventType.ALERT_CREATED, message.getType());
        assertNotNull(message.getTimestamp());
        assertEquals("alert-123", message.getPayload().get("alertId"));
        assertEquals("HIGH", message.getPayload().get("severity"));
    }

    @Test
    void setters_shouldUpdateFields() {
        DashboardEventMessage message = new DashboardEventMessage();

        message.setType(DashboardEventType.SECURITY_EVENT_CREATED);
        message.setPayload(Map.of("eventId", "event-123"));

        assertEquals(DashboardEventType.SECURITY_EVENT_CREATED, message.getType());
        assertEquals("event-123", message.getPayload().get("eventId"));
    }
}