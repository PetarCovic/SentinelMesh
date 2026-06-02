package com.sentinelmesh.realtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DashboardWebSocketHandlerTest {

    private DashboardWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        handler = new DashboardWebSocketHandler();
    }

    @Test
    void afterConnectionEstablished_shouldTrackSession() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-1");

        handler.afterConnectionEstablished(session);

        assertEquals(1, handler.getActiveSessionCount());
    }

    @Test
    void afterConnectionClosed_shouldRemoveSession() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-1");

        handler.afterConnectionEstablished(session);
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        assertEquals(0, handler.getActiveSessionCount());
    }

    @Test
    void broadcast_shouldSendMessageToOpenSession() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-1");
        when(session.isOpen()).thenReturn(true);

        handler.afterConnectionEstablished(session);

        handler.broadcast("{\"type\":\"ALERT_CREATED\"}");

        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    void broadcast_shouldRemoveClosedSession() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-1");
        when(session.isOpen()).thenReturn(false);

        handler.afterConnectionEstablished(session);

        handler.broadcast("{\"type\":\"ALERT_CREATED\"}");

        assertEquals(0, handler.getActiveSessionCount());
        verify(session, never()).sendMessage(any(TextMessage.class));
    }
}