export type DashboardEventType =
  | "DEVICE_REGISTERED"
  | "DEVICE_HEARTBEAT_RECEIVED"
  | "DEVICE_STATUS_CHANGED"
  | "SECURITY_EVENT_CREATED"
  | "ALERT_CREATED"
  | "ALERT_ACKNOWLEDGED"
  | "ALERT_RESOLVED"
  | "RULE_CREATED"
  | "RULE_UPDATED"
  | "RULE_DELETED";

export interface DashboardEventMessage {
  type: DashboardEventType;
  timestamp: string;
  payload: Record<string, unknown>;
}

export function createDashboardWebSocket(
  onMessage: (message: DashboardEventMessage) => void,
  onOpen?: () => void,
  onClose?: () => void,
  onError?: () => void
): WebSocket {
  const socket = new WebSocket("ws://localhost:8080/ws/dashboard");

  socket.onopen = () => {
    onOpen?.();
  };

  socket.onmessage = (event) => {
    try {
      const message = JSON.parse(event.data) as DashboardEventMessage;
      onMessage(message);
    } catch (error) {
      console.error("Failed to parse dashboard WebSocket message", error);
    }
  };

  socket.onclose = () => {
    onClose?.();
  };

  socket.onerror = () => {
    onError?.();
  };

  return socket;
}