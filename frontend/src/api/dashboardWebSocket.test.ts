import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import { createDashboardWebSocket } from "./dashboardWebSocket";

class MockWebSocket {
  static instances: MockWebSocket[] = [];

  onopen: (() => void) | null = null;
  onmessage: ((event: MessageEvent) => void) | null = null;
  onclose: (() => void) | null = null;
  onerror: (() => void) | null = null;

  url: string;
  close = vi.fn();

  constructor(url: string) {
    this.url = url;
    MockWebSocket.instances.push(this);
  }

  triggerOpen() {
    this.onopen?.();
  }

  triggerMessage(data: unknown) {
    this.onmessage?.({
      data: JSON.stringify(data),
    } as MessageEvent);
  }

  triggerInvalidMessage(data: string) {
    this.onmessage?.({
      data,
    } as MessageEvent);
  }

  triggerClose() {
    this.onclose?.();
  }

  triggerError() {
    this.onerror?.();
  }
}

describe("dashboardWebSocket", () => {
  beforeEach(() => {
    MockWebSocket.instances = [];
    vi.stubGlobal("WebSocket", MockWebSocket);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  test("connects to dashboard WebSocket URL", () => {
    createDashboardWebSocket(vi.fn());

    expect(MockWebSocket.instances).toHaveLength(1);
    expect(MockWebSocket.instances[0].url).toBe("ws://localhost:8080/ws/dashboard");
  });

  test("calls onOpen when socket opens", () => {
    const onOpen = vi.fn();

    createDashboardWebSocket(vi.fn(), onOpen);

    MockWebSocket.instances[0].triggerOpen();

    expect(onOpen).toHaveBeenCalledTimes(1);
  });

  test("parses incoming JSON messages", () => {
    const onMessage = vi.fn();

    createDashboardWebSocket(onMessage);

    MockWebSocket.instances[0].triggerMessage({
      type: "ALERT_CREATED",
      timestamp: "2026-06-01T12:00:00Z",
      payload: {
        alertId: "alert-123",
      },
    });

    expect(onMessage).toHaveBeenCalledWith({
      type: "ALERT_CREATED",
      timestamp: "2026-06-01T12:00:00Z",
      payload: {
        alertId: "alert-123",
      },
    });
  });

  test("calls onClose when socket closes", () => {
    const onClose = vi.fn();

    createDashboardWebSocket(vi.fn(), undefined, onClose);

    MockWebSocket.instances[0].triggerClose();

    expect(onClose).toHaveBeenCalledTimes(1);
  });

  test("calls onError when socket errors", () => {
    const onError = vi.fn();

    createDashboardWebSocket(vi.fn(), undefined, undefined, onError);

    MockWebSocket.instances[0].triggerError();

    expect(onError).toHaveBeenCalledTimes(1);
  });

  test("does not call onMessage when incoming message is invalid JSON", () => {
    const onMessage = vi.fn();

    createDashboardWebSocket(onMessage);

    MockWebSocket.instances[0].triggerInvalidMessage("not valid json");

    expect(onMessage).not.toHaveBeenCalled();
  });

  test("returns the created WebSocket instance", () => {
    const socket = createDashboardWebSocket(vi.fn());

    expect(socket).toBe(MockWebSocket.instances[0]);
  });
});