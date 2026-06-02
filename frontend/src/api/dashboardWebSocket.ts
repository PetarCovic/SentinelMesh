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

export interface DashboardWebSocketClient {
  close: () => void;
}

const DASHBOARD_WS_URL = "ws://localhost:8080/ws/dashboard";

export function createDashboardWebSocket(
  onMessage: (message: DashboardEventMessage) => void,
  onOpen?: () => void,
  onClose?: () => void,
  onError?: () => void
): DashboardWebSocketClient {
  let socket: WebSocket | null = null;
  let reconnectTimeoutId: number | null = null;
  let manuallyClosed = false;

  function connect() {
    socket = new WebSocket(DASHBOARD_WS_URL);

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

      if (!manuallyClosed) {
        reconnectTimeoutId = window.setTimeout(() => {
          connect();
        }, 3000);
      }
    };

    socket.onerror = () => {
      onError?.();
    };
  }

  connect();

  return {
    close: () => {
      manuallyClosed = true;

      if (reconnectTimeoutId !== null) {
        window.clearTimeout(reconnectTimeoutId);
      }

      socket?.close();
    },
  };
}