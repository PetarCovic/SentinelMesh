package com.sentinelmesh.realtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

class DashboardEventBroadcasterTest {

    private DashboardWebSocketHandler dashboardWebSocketHandler;
    private DashboardEventBroadcaster dashboardEventBroadcaster;

    @BeforeEach
    void setUp() {
        dashboardWebSocketHandler = mock(DashboardWebSocketHandler.class);
        ObjectMapper objectMapper = new ObjectMapper();

        dashboardEventBroadcaster = new DashboardEventBroadcaster(
                dashboardWebSocketHandler,
                objectMapper
        );
    }

    @Test
    void broadcast_shouldSerializeAndSendDashboardEvent() {
        dashboardEventBroadcaster.broadcast(
                DashboardEventType.ALERT_CREATED,
                Map.of(
                        "alertId", "alert-123",
                        "severity", "HIGH"
                )
        );

        verify(dashboardWebSocketHandler).broadcast(contains("ALERT_CREATED"));
        verify(dashboardWebSocketHandler).broadcast(contains("alert-123"));
        verify(dashboardWebSocketHandler).broadcast(contains("HIGH"));
    }

    @Test
    void getActiveSessionCount_shouldDelegateToHandler() {
        when(dashboardWebSocketHandler.getActiveSessionCount()).thenReturn(3);

        int count = dashboardEventBroadcaster.getActiveSessionCount();

        assertEquals(3, count);
    }
}