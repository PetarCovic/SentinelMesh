import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import App from "./App";
import {
  acknowledgeAlert,
  getDevices,
  getOpenAlertsPaged,
  getRecentAlerts,
  getRecentEvents,
  resolveAlert,
} from "./api/sentinelMeshApi";
import type { Alert, Device, PageResponse, SecurityEvent } from "./types";

vi.mock("./api/sentinelMeshApi", () => ({
  getDevices: vi.fn(),
  getRecentEvents: vi.fn(),
  getRecentAlerts: vi.fn(),
  getOpenAlertsPaged: vi.fn(),
  acknowledgeAlert: vi.fn(),
  resolveAlert: vi.fn(),
}));

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

  triggerClose() {
    this.onclose?.();
  }

  triggerError() {
    this.onerror?.();
  }
}

const mockGetDevices = vi.mocked(getDevices);
const mockGetRecentEvents = vi.mocked(getRecentEvents);
const mockGetRecentAlerts = vi.mocked(getRecentAlerts);
const mockGetOpenAlertsPaged = vi.mocked(getOpenAlertsPaged);
const mockAcknowledgeAlert = vi.mocked(acknowledgeAlert);
const mockResolveAlert = vi.mocked(resolveAlert);

const devices: Device[] = [
  {
    id: "device-1",
    name: "Front Door Camera",
    type: "CAMERA",
    location: "Front Porch",
    status: "ONLINE",
    lastSeenAt: "2026-05-27T20:00:00Z",
    createdAt: "2026-05-27T19:00:00Z",
    updatedAt: "2026-05-27T20:00:00Z",
  },
  {
    id: "device-2",
    name: "Garage Camera",
    type: "CAMERA",
    location: "Garage",
    status: "OFFLINE",
    lastSeenAt: null,
    createdAt: "2026-05-27T19:00:00Z",
    updatedAt: "2026-05-27T19:00:00Z",
  },
];

const events: SecurityEvent[] = [
  {
    id: "event-1",
    deviceId: "device-1",
    deviceName: "Front Door Camera",
    eventType: "PERSON_DETECTED",
    severity: "HIGH",
    confidence: 0.93,
    occurredAt: "2026-05-27T20:05:00Z",
    receivedAt: "2026-05-27T20:05:01Z",
    metadataJson: null,
  },
  {
    id: "event-2",
    deviceId: "device-2",
    deviceName: "Garage Camera",
    eventType: "MOTION_DETECTED",
    severity: "MEDIUM",
    confidence: 0.75,
    occurredAt: "2026-05-27T20:06:00Z",
    receivedAt: "2026-05-27T20:06:01Z",
    metadataJson: null,
  },
];

const alerts: Alert[] = [
  {
    id: "alert-1",
    securityEventId: "event-1",
    deviceId: "device-1",
    deviceName: "Front Door Camera",
    severity: "HIGH",
    status: "OPEN",
    title: "HIGH security event: PERSON_DETECTED",
    message: "Device Front Door Camera reported PERSON_DETECTED with severity HIGH.",
    createdAt: "2026-05-27T20:05:02Z",
    acknowledgedAt: null,
    resolvedAt: null,
  },
  {
    id: "alert-2",
    securityEventId: "event-2",
    deviceId: "device-2",
    deviceName: "Garage Camera",
    severity: "CRITICAL",
    status: "RESOLVED",
    title: "CRITICAL security event: MOTION_DETECTED",
    message: "Device Garage Camera reported MOTION_DETECTED with severity CRITICAL.",
    createdAt: "2026-05-27T20:06:02Z",
    acknowledgedAt: null,
    resolvedAt: "2026-05-27T20:10:00Z",
  },
];

const openAlerts: Alert[] = [alerts[0]];

function pageResponse<T>(content: T[]): PageResponse<T> {
  return {
    content,
    page: 0,
    size: 50,
    totalElements: content.length,
    totalPages: content.length === 0 ? 0 : 1,
    first: true,
    last: true,
    hasNext: false,
    hasPrevious: false,
  };
}

function mockSuccessfulDashboardLoad() {
  mockGetDevices.mockResolvedValue(devices);
  mockGetRecentEvents.mockResolvedValue(pageResponse(events));
  mockGetRecentAlerts.mockResolvedValue(pageResponse(alerts));
  mockGetOpenAlertsPaged.mockResolvedValue(pageResponse(openAlerts));

  mockAcknowledgeAlert.mockResolvedValue({
    ...alerts[0],
    status: "ACKNOWLEDGED",
    acknowledgedAt: "2026-05-27T20:15:00Z",
  });

  mockResolveAlert.mockResolvedValue({
    ...alerts[0],
    status: "RESOLVED",
    resolvedAt: "2026-05-27T20:16:00Z",
  });
}

describe("App dashboard", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    MockWebSocket.instances = [];
    vi.stubGlobal("WebSocket", MockWebSocket);
    mockSuccessfulDashboardLoad();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  test("renders the dashboard title", async () => {
    render(<App />);

    expect(
      screen.getByRole("heading", { name: /security operations dashboard/i })
    ).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getAllByText("Front Door Camera").length).toBeGreaterThan(0);
    });
  });

  test("shows loading state before data loads", () => {
    render(<App />);

    expect(screen.getByText(/loading dashboard/i)).toBeInTheDocument();
  });

  test("loads and displays devices, events, and alerts", async () => {
    render(<App />);

    await waitFor(() => {
      expect(screen.getAllByText("Front Door Camera").length).toBeGreaterThan(0);
    });

    expect(screen.getAllByText("Garage Camera").length).toBeGreaterThan(0);
    expect(screen.getByText("PERSON_DETECTED")).toBeInTheDocument();
    expect(screen.getByText("MOTION_DETECTED")).toBeInTheDocument();
    expect(
      screen.getAllByText("HIGH security event: PERSON_DETECTED").length
    ).toBeGreaterThan(0);

    expect(mockGetDevices).toHaveBeenCalledTimes(1);
    expect(mockGetRecentEvents).toHaveBeenCalledWith(0, 50);
    expect(mockGetRecentAlerts).toHaveBeenCalledWith(0, 50);
    expect(mockGetOpenAlertsPaged).toHaveBeenCalledWith(0, 50);
  });

  test("acknowledge button calls acknowledge API and reloads dashboard", async () => {
    const user = userEvent.setup();

    render(<App />);

    await waitFor(() => {
      expect(
        screen.getAllByText("HIGH security event: PERSON_DETECTED").length
      ).toBeGreaterThan(0);
    });

    await user.click(screen.getByRole("button", { name: /acknowledge/i }));

    expect(mockAcknowledgeAlert).toHaveBeenCalledWith("alert-1");

    await waitFor(() => {
      expect(mockGetDevices).toHaveBeenCalledTimes(2);
      expect(mockGetRecentEvents).toHaveBeenCalledTimes(2);
      expect(mockGetRecentAlerts).toHaveBeenCalledTimes(2);
      expect(mockGetOpenAlertsPaged).toHaveBeenCalledTimes(2);
    });
  });

  test("resolve button calls resolve API and reloads dashboard", async () => {
    const user = userEvent.setup();

    render(<App />);

    await waitFor(() => {
      expect(
        screen.getAllByText("HIGH security event: PERSON_DETECTED").length
      ).toBeGreaterThan(0);
    });

    await user.click(screen.getByRole("button", { name: /resolve/i }));

    expect(mockResolveAlert).toHaveBeenCalledWith("alert-1");

    await waitFor(() => {
      expect(mockGetDevices).toHaveBeenCalledTimes(2);
      expect(mockGetRecentEvents).toHaveBeenCalledTimes(2);
      expect(mockGetRecentAlerts).toHaveBeenCalledTimes(2);
      expect(mockGetOpenAlertsPaged).toHaveBeenCalledTimes(2);
    });
  });

  test("shows an error message when dashboard loading fails", async () => {
    mockGetDevices.mockRejectedValue(new Error("Backend unavailable"));

    render(<App />);

    await waitFor(() => {
      expect(screen.getByText(/backend unavailable/i)).toBeInTheDocument();
    });
  });

  test("pagination next button requests the next event page", async () => {
    const user = userEvent.setup();

    mockGetRecentEvents.mockResolvedValue({
      content: events,
      page: 0,
      size: 50,
      totalElements: 100,
      totalPages: 2,
      first: true,
      last: false,
      hasNext: true,
      hasPrevious: false,
    });

    render(<App />);

    await waitFor(() => {
      expect(screen.getByText(/page 1 of 2/i)).toBeInTheDocument();
    });

    await user.click(screen.getAllByRole("button", { name: /next/i })[0]);

    await waitFor(() => {
      expect(mockGetRecentEvents).toHaveBeenCalledWith(1, 50);
    });
  });

  test("auto-refresh can be paused and resumed", async () => {
    const user = userEvent.setup();

    render(<App />);

    await waitFor(() => {
      expect(screen.getAllByText("Front Door Camera").length).toBeGreaterThan(0);
    });

    const pauseButton = screen.getByRole("button", {
      name: /pause auto-refresh/i,
    });

    await user.click(pauseButton);

    expect(
      screen.getByRole("button", { name: /resume auto-refresh/i })
    ).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /resume auto-refresh/i }));

    expect(
      screen.getByRole("button", { name: /pause auto-refresh/i })
    ).toBeInTheDocument();
  });

  test("creates a WebSocket connection to the dashboard endpoint", async () => {
    render(<App />);

    await waitFor(() => {
      expect(MockWebSocket.instances.length).toBe(1);
    });

    expect(MockWebSocket.instances[0].url).toBe("ws://localhost:8080/ws/dashboard");
  });

  test("shows WebSocket connected when socket opens", async () => {
    render(<App />);

    await waitFor(() => {
      expect(MockWebSocket.instances.length).toBe(1);
    });

    MockWebSocket.instances[0].triggerOpen();

    await waitFor(() => {
      expect(screen.getByText(/websocket: connected/i)).toBeInTheDocument();
    });
  });

  test("shows WebSocket disconnected when socket closes", async () => {
    render(<App />);

    await waitFor(() => {
      expect(MockWebSocket.instances.length).toBe(1);
    });

    MockWebSocket.instances[0].triggerOpen();

    await waitFor(() => {
      expect(screen.getByText(/websocket: connected/i)).toBeInTheDocument();
    });

    MockWebSocket.instances[0].triggerClose();

    await waitFor(() => {
      expect(screen.getByText(/websocket: disconnected/i)).toBeInTheDocument();
    });
  });

  test("shows WebSocket disconnected when socket errors", async () => {
    render(<App />);

    await waitFor(() => {
      expect(MockWebSocket.instances.length).toBe(1);
    });

    MockWebSocket.instances[0].triggerOpen();

    await waitFor(() => {
      expect(screen.getByText(/websocket: connected/i)).toBeInTheDocument();
    });

    MockWebSocket.instances[0].triggerError();

    await waitFor(() => {
      expect(screen.getByText(/websocket: disconnected/i)).toBeInTheDocument();
    });
  });

  test("reloads alert data when ALERT_CREATED WebSocket message is received", async () => {
  render(<App />);

  await waitFor(() => {
    expect(screen.getAllByText("Front Door Camera").length).toBeGreaterThan(0);
  });

  expect(mockGetDevices).toHaveBeenCalledTimes(1);
  expect(mockGetRecentEvents).toHaveBeenCalledTimes(1);
  expect(mockGetRecentAlerts).toHaveBeenCalledTimes(1);
  expect(mockGetOpenAlertsPaged).toHaveBeenCalledTimes(1);

  MockWebSocket.instances[0].triggerMessage({
    type: "ALERT_CREATED",
    timestamp: new Date().toISOString(),
    payload: {
      alertId: "alert-123",
      severity: "HIGH",
    },
  });

  await waitFor(() => {
    expect(mockGetRecentAlerts).toHaveBeenCalledTimes(2);
    expect(mockGetOpenAlertsPaged).toHaveBeenCalledTimes(2);
  });

  expect(mockGetDevices).toHaveBeenCalledTimes(1);
  expect(mockGetRecentEvents).toHaveBeenCalledTimes(1);

  await waitFor(() => {
    expect(screen.getByText(/last event: alert_created/i)).toBeInTheDocument();
  });
});

  test("closes WebSocket when dashboard unmounts", async () => {
    const { unmount } = render(<App />);

    await waitFor(() => {
      expect(MockWebSocket.instances.length).toBe(1);
    });

    const socket = MockWebSocket.instances[0];

    unmount();

    expect(socket.close).toHaveBeenCalled();
  });
});