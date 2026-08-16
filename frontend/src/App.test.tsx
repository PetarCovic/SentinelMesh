import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import App from "./App";
import {
  acknowledgeAlert,
  createAlertRule,
  deleteAlertRule,
  disableAlertRule,
  enableAlertRule,
  getAlertRules,
  getDevices,
  getOpenAlertsPaged,
  getRecentAlerts,
  getRecentEvents,
  resolveAlert,
  updateAlertRule,
} from "./api/sentinelMeshApi";
import type {
  Alert,
  AlertRule,
  Device,
  PageResponse,
  SecurityEvent,
} from "./types";

vi.mock("./api/sentinelMeshApi", () => ({
  buildApiUrl: vi.fn((path: string | null | undefined) => {
    if (!path) {
      return null;
    }

    if (path.startsWith("http://") || path.startsWith("https://")) {
      return path;
    }

    return `http://localhost:8080${path}`;
  }),

  getDevices: vi.fn(),
  getRecentEvents: vi.fn(),
  getRecentAlerts: vi.fn(),
  getOpenAlertsPaged: vi.fn(),
  acknowledgeAlert: vi.fn(),
  resolveAlert: vi.fn(),

  getAlertRules: vi.fn(),
  createAlertRule: vi.fn(),
  updateAlertRule: vi.fn(),
  deleteAlertRule: vi.fn(),
  enableAlertRule: vi.fn(),
  disableAlertRule: vi.fn(),
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

const mockGetAlertRules = vi.mocked(getAlertRules);
const mockCreateAlertRule = vi.mocked(createAlertRule);
const mockUpdateAlertRule = vi.mocked(updateAlertRule);
const mockDeleteAlertRule = vi.mocked(deleteAlertRule);
const mockEnableAlertRule = vi.mocked(enableAlertRule);
const mockDisableAlertRule = vi.mocked(disableAlertRule);

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
    snapshotId: null,
    videoClipId: null,
    deviceName: "Front Door Camera",
    eventType: "PERSON_DETECTED",
    severity: "HIGH",
    confidence: 0.93,
    occurredAt: "2026-05-27T20:05:00Z",
    receivedAt: "2026-05-27T20:05:01Z",
    metadataJson: null,
    snapshotImageUrl: null,
    videoClipUrl: null,
    snapshotAvailable: false,
    videoClipAvailable: false,
  },
  {
    id: "event-2",
    deviceId: "device-2",
    snapshotId: null,
    videoClipId: null,
    deviceName: "Garage Camera",
    eventType: "MOTION_DETECTED",
    severity: "MEDIUM",
    confidence: 0.75,
    occurredAt: "2026-05-27T20:06:00Z",
    receivedAt: "2026-05-27T20:06:01Z",
    metadataJson: null,
    snapshotImageUrl: null,
    videoClipUrl: null,
    snapshotAvailable: false,
    videoClipAvailable: false,
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

const rules: AlertRule[] = [
  {
    id: "rule-1",
    name: "High severity events",
    description: "Creates alerts for HIGH and CRITICAL events",
    enabled: true,
    eventType: null,
    minimumSeverity: "HIGH",
    deviceType: null,
    alertSeverity: "HIGH",
    alertTitle: "{severity} security event: {eventType}",
    alertMessage:
      "Device {deviceName} reported {eventType} with severity {severity}.",
    createdAt: "2026-05-27T20:00:00Z",
    updatedAt: "2026-05-27T20:00:00Z",
  },
  {
    id: "rule-2",
    name: "Person detected",
    description: "Creates alerts for person detections",
    enabled: false,
    eventType: "PERSON_DETECTED",
    minimumSeverity: "MEDIUM",
    deviceType: "CAMERA",
    alertSeverity: "CRITICAL",
    alertTitle: "Person detected by {deviceName}",
    alertMessage: "{deviceName} detected a person with severity {severity}.",
    createdAt: "2026-05-27T20:01:00Z",
    updatedAt: "2026-05-27T20:01:00Z",
  },
];

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
  mockGetAlertRules.mockResolvedValue(rules);

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

  mockCreateAlertRule.mockResolvedValue(rules[0]);

  mockUpdateAlertRule.mockResolvedValue({
    ...rules[0],
    name: "Updated Rule",
  });

  mockDeleteAlertRule.mockResolvedValue(undefined);

  mockEnableAlertRule.mockResolvedValue({
    ...rules[1],
    enabled: true,
  });

  mockDisableAlertRule.mockResolvedValue({
    ...rules[0],
    enabled: false,
  });
}

async function waitForDashboardToLoad() {
  await waitFor(() => {
    expect(screen.getAllByText("Front Door Camera").length).toBeGreaterThan(0);
  });
}

function getSectionByHeading(name: RegExp | string) {
  const heading = screen.getByRole("heading", { name });
  const section = heading.closest("section");

  if (!section) {
    throw new Error(`Could not find section for heading: ${name}`);
  }

  return within(section);
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

    await waitForDashboardToLoad();
  });

  test("shows loading state before data loads", () => {
    render(<App />);

    expect(screen.getByText(/loading dashboard/i)).toBeInTheDocument();
  });

  test("loads and displays devices, events, alerts, and rules", async () => {
    render(<App />);

    await waitForDashboardToLoad();

    expect(screen.getAllByText("Garage Camera").length).toBeGreaterThan(0);
    expect(screen.getAllByText("PERSON_DETECTED").length).toBeGreaterThan(0);
    expect(screen.getAllByText("MOTION_DETECTED").length).toBeGreaterThan(0);

    expect(
      screen.getAllByText("HIGH security event: PERSON_DETECTED").length
    ).toBeGreaterThan(0);

    expect(screen.getByText("High severity events")).toBeInTheDocument();
    expect(screen.getByText("Person detected")).toBeInTheDocument();

    expect(mockGetDevices).toHaveBeenCalledTimes(1);
    expect(mockGetRecentEvents).toHaveBeenCalledWith(0, 50);
    expect(mockGetRecentAlerts).toHaveBeenCalledWith(0, 50);
    expect(mockGetOpenAlertsPaged).toHaveBeenCalledWith(0, 50);
    expect(mockGetAlertRules).toHaveBeenCalledTimes(1);
  });

  test("acknowledge button calls acknowledge API and reloads alert data", async () => {
    const user = userEvent.setup();

    render(<App />);

    await waitForDashboardToLoad();

    await user.click(screen.getByRole("button", { name: /acknowledge/i }));

    expect(mockAcknowledgeAlert).toHaveBeenCalledWith("alert-1");

    await waitFor(() => {
      expect(mockGetRecentAlerts).toHaveBeenCalledTimes(2);
      expect(mockGetOpenAlertsPaged).toHaveBeenCalledTimes(2);
    });
  });

  test("resolve button calls resolve API and reloads alert data", async () => {
    const user = userEvent.setup();

    render(<App />);

    await waitForDashboardToLoad();

    await user.click(screen.getByRole("button", { name: /resolve/i }));

    expect(mockResolveAlert).toHaveBeenCalledWith("alert-1");

    await waitFor(() => {
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

    await waitForDashboardToLoad();

    await user.click(
      screen.getByRole("button", { name: /pause auto-refresh/i })
    );

    expect(
      screen.getByRole("button", { name: /resume auto-refresh/i })
    ).toBeInTheDocument();

    await user.click(
      screen.getByRole("button", { name: /resume auto-refresh/i })
    );

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

    await waitForDashboardToLoad();

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

  test("reloads rules when RULE_UPDATED WebSocket message is received", async () => {
    render(<App />);

    await waitForDashboardToLoad();

    expect(mockGetAlertRules).toHaveBeenCalledTimes(1);

    MockWebSocket.instances[0].triggerMessage({
      type: "RULE_UPDATED",
      timestamp: new Date().toISOString(),
      payload: {
        ruleId: "rule-1",
      },
    });

    await waitFor(() => {
      expect(mockGetAlertRules).toHaveBeenCalledTimes(2);
    });

    expect(screen.getByText(/last event: rule_updated/i)).toBeInTheDocument();
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

  test("filters devices by search text", async () => {
  const user = userEvent.setup();

  render(<App />);

  await waitForDashboardToLoad();

  const devicesSection = getSectionByHeading(/devices/i);

  await user.type(devicesSection.getByPlaceholderText(/search devices/i), "garage");

  expect(devicesSection.getByText(/showing 1 of 2/i)).toBeInTheDocument();

  expect(
    devicesSection.getAllByText("Garage Camera").length
  ).toBeGreaterThan(0);
});

  test("filters devices by status", async () => {
  const user = userEvent.setup();

  render(<App />);

  await waitForDashboardToLoad();

  const devicesSection = getSectionByHeading(/devices/i);

  await user.selectOptions(
    devicesSection.getByDisplayValue("All statuses"),
    "OFFLINE"
  );

  expect(devicesSection.getByText(/showing 1 of 2/i)).toBeInTheDocument();

  expect(
    devicesSection.getAllByText("Garage Camera").length
  ).toBeGreaterThan(0);
});

  test("filters events by severity", async () => {
  const user = userEvent.setup();

  render(<App />);

  await waitForDashboardToLoad();

  const recentEventsSection = getSectionByHeading(/recent events/i);

  await user.selectOptions(
    recentEventsSection.getByDisplayValue("All severities"),
    "HIGH"
  );

  expect(recentEventsSection.getByText(/showing 1 of 2/i)).toBeInTheDocument();
  expect(recentEventsSection.getAllByText("PERSON_DETECTED").length).toBeGreaterThan(0);
});

  test("filters events by event type", async () => {
  const user = userEvent.setup();

  render(<App />);

  await waitForDashboardToLoad();

  const recentEventsSection = getSectionByHeading(/recent events/i);

  await user.selectOptions(
    recentEventsSection.getByDisplayValue("All event types"),
    "MOTION_DETECTED"
  );

  expect(recentEventsSection.getByText(/showing 1 of 2/i)).toBeInTheDocument();
  expect(recentEventsSection.getAllByText("MOTION_DETECTED").length).toBeGreaterThan(0);
});

  test("filters recent alerts by status", async () => {
    const user = userEvent.setup();

    render(<App />);

    await waitForDashboardToLoad();

    const recentAlertsSection = getSectionByHeading(/recent alerts/i);

    expect(
      recentAlertsSection.getAllByText("HIGH security event: PERSON_DETECTED").length
    ).toBeGreaterThan(0);

    await user.selectOptions(
      recentAlertsSection.getByDisplayValue("All statuses"),
      "RESOLVED"
    );

    expect(
      recentAlertsSection.queryByText("HIGH security event: PERSON_DETECTED")
    ).not.toBeInTheDocument();

    expect(
      recentAlertsSection.getByText("CRITICAL security event: MOTION_DETECTED")
    ).toBeInTheDocument();
  });

  test("filters recent alerts by severity", async () => {
    const user = userEvent.setup();

    render(<App />);

    await waitForDashboardToLoad();

    const recentAlertsSection = getSectionByHeading(/recent alerts/i);

    expect(
      recentAlertsSection.getAllByText("HIGH security event: PERSON_DETECTED").length
    ).toBeGreaterThan(0);

    await user.selectOptions(
      recentAlertsSection.getByDisplayValue("All severities"),
      "CRITICAL"
    );

    expect(
      recentAlertsSection.queryByText("HIGH security event: PERSON_DETECTED")
    ).not.toBeInTheDocument();

    expect(
      recentAlertsSection.getByText("CRITICAL security event: MOTION_DETECTED")
    ).toBeInTheDocument();
  });

  test("filters rules by search text", async () => {
    const user = userEvent.setup();

    render(<App />);

    await waitForDashboardToLoad();

    const rulesSection = getSectionByHeading(/alert rules/i);

    await user.type(rulesSection.getByPlaceholderText(/search rules/i), "person");

    expect(rulesSection.queryByText("High severity events")).not.toBeInTheDocument();
    expect(rulesSection.getByText("Person detected")).toBeInTheDocument();
  });

  test("filters rules by enabled status", async () => {
    const user = userEvent.setup();

    render(<App />);

    await waitForDashboardToLoad();

    const rulesSection = getSectionByHeading(/alert rules/i);

    await user.selectOptions(rulesSection.getByDisplayValue("All rules"), "DISABLED");

    expect(rulesSection.queryByText("High severity events")).not.toBeInTheDocument();
    expect(rulesSection.getByText("Person detected")).toBeInTheDocument();
  });

  test("changes device sort direction", async () => {
  const user = userEvent.setup();

  render(<App />);

  await waitForDashboardToLoad();

  const devicesSection = getSectionByHeading(/devices/i);

  const directionButton = devicesSection.getByRole("button", {
    name: /asc/i,
  });

  await user.click(directionButton);

  expect(directionButton).toHaveTextContent(/desc/i);
});

  test("changes event sort field and direction controls", async () => {
  const user = userEvent.setup();

  render(<App />);

  await waitForDashboardToLoad();

  const recentEventsSection = getSectionByHeading(/recent events/i);

  const eventSortDropdown = recentEventsSection.getByDisplayValue("Sort by received");

  await user.selectOptions(eventSortDropdown, "confidence");

  expect(eventSortDropdown).toHaveValue("confidence");

  const directionButton = recentEventsSection.getByRole("button", {
    name: /desc/i,
  });

  await user.click(directionButton);

  expect(directionButton).toHaveTextContent(/asc/i);
});

  test("changes rule sort field and direction controls", async () => {
  const user = userEvent.setup();

  render(<App />);

  await waitForDashboardToLoad();

  const rulesSection = getSectionByHeading(/alert rules/i);

  const ruleSortDropdown = rulesSection.getByDisplayValue("Sort by name");

  await user.selectOptions(ruleSortDropdown, "alertSeverity");

  expect(ruleSortDropdown).toHaveValue("alertSeverity");

  const directionButton = rulesSection.getByRole("button", {
    name: /asc/i,
  });

  await user.click(directionButton);

  expect(directionButton).toHaveTextContent(/desc/i);
  });
});