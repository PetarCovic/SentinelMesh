import { useCallback, useEffect, useMemo, useState } from "react";
import type { FormEvent } from "react";
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
import {
  createDashboardWebSocket,
  type DashboardEventMessage,
} from "./api/dashboardWebSocket";
import type {
  Alert,
  AlertRule,
  AlertRuleDeviceType,
  AlertRuleEventType,
  AlertRuleSeverity,
  CreateAlertRuleRequest,
  Device,
  PageResponse,
  SecurityEvent,
} from "./types";
import "./App.css";

const PAGE_SIZE = 50;

type DeviceStatusFilter = "ALL" | "ONLINE" | "OFFLINE";
type SeverityFilter = "ALL" | "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
type EventTypeFilter =
  | "ALL"
  | "MOTION_DETECTED"
  | "PERSON_DETECTED"
  | "DOOR_OPENED"
  | "SOUND_DETECTED";
type AlertStatusFilter = "ALL" | "OPEN" | "ACKNOWLEDGED" | "RESOLVED";
type RuleEnabledFilter = "ALL" | "ENABLED" | "DISABLED";

type SortDirection = "asc" | "desc";

type DeviceSortField = "name" | "type" | "location" | "status" | "lastSeenAt";
type EventSortField = "receivedAt" | "eventType" | "severity" | "deviceName" | "confidence";
type AlertSortField = "createdAt" | "severity" | "status" | "deviceName" | "title";
type RuleSortField = "name" | "enabled" | "eventType" | "minimumSeverity" | "deviceType" | "alertSeverity";

const defaultRuleForm: CreateAlertRuleRequest = {
  name: "",
  description: "",
  enabled: true,
  eventType: null,
  minimumSeverity: "HIGH",
  deviceType: null,
  alertSeverity: "HIGH",
  alertTitle: "{severity} security event: {eventType}",
  alertMessage:
    "Device {deviceName} reported {eventType} with severity {severity} at {deviceLocation}.",
};

const severityRank: Record<string, number> = {
  LOW: 1,
  MEDIUM: 2,
  HIGH: 3,
  CRITICAL: 4,
};

function App() {
  const [devices, setDevices] = useState<Device[]>([]);
  const [events, setEvents] = useState<SecurityEvent[]>([]);
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [openAlerts, setOpenAlerts] = useState<Alert[]>([]);
  const [rules, setRules] = useState<AlertRule[]>([]);

  const [eventsPage, setEventsPage] = useState(0);
  const [alertsPage, setAlertsPage] = useState(0);

  const [eventsPageData, setEventsPageData] =
    useState<PageResponse<SecurityEvent> | null>(null);

  const [alertsPageData, setAlertsPageData] =
    useState<PageResponse<Alert> | null>(null);

  const [openAlertsPageData, setOpenAlertsPageData] =
    useState<PageResponse<Alert> | null>(null);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [autoRefreshEnabled, setAutoRefreshEnabled] = useState(true);
  const [lastUpdatedAt, setLastUpdatedAt] = useState<Date | null>(null);

  const [webSocketConnected, setWebSocketConnected] = useState(false);
  const [lastWebSocketMessage, setLastWebSocketMessage] =
    useState<DashboardEventMessage | null>(null);

  const [ruleForm, setRuleForm] =
    useState<CreateAlertRuleRequest>(defaultRuleForm);

  const [editingRuleId, setEditingRuleId] = useState<string | null>(null);

  const [deviceSearch, setDeviceSearch] = useState("");
  const [deviceStatusFilter, setDeviceStatusFilter] =
    useState<DeviceStatusFilter>("ALL");
  const [deviceSortField, setDeviceSortField] =
    useState<DeviceSortField>("name");
  const [deviceSortDirection, setDeviceSortDirection] =
    useState<SortDirection>("asc");

  const [eventSearch, setEventSearch] = useState("");
  const [eventSeverityFilter, setEventSeverityFilter] =
    useState<SeverityFilter>("ALL");
  const [eventTypeFilter, setEventTypeFilter] =
    useState<EventTypeFilter>("ALL");
  const [eventSortField, setEventSortField] =
    useState<EventSortField>("receivedAt");
  const [eventSortDirection, setEventSortDirection] =
    useState<SortDirection>("desc");

  const [alertSearch, setAlertSearch] = useState("");
  const [alertStatusFilter, setAlertStatusFilter] =
    useState<AlertStatusFilter>("ALL");
  const [alertSeverityFilter, setAlertSeverityFilter] =
    useState<SeverityFilter>("ALL");
  const [alertSortField, setAlertSortField] =
    useState<AlertSortField>("createdAt");
  const [alertSortDirection, setAlertSortDirection] =
    useState<SortDirection>("desc");

  const [ruleSearch, setRuleSearch] = useState("");
  const [ruleEnabledFilter, setRuleEnabledFilter] =
    useState<RuleEnabledFilter>("ALL");
  const [ruleSortField, setRuleSortField] =
    useState<RuleSortField>("name");
  const [ruleSortDirection, setRuleSortDirection] =
    useState<SortDirection>("asc");

  const loadDashboardData = useCallback(
    async (showLoading = false) => {
      try {
        if (showLoading) {
          setLoading(true);
        }

        setError(null);

        const [devicesData, eventsData, alertsData, openAlertsData, rulesData] =
          await Promise.all([
            getDevices(),
            getRecentEvents(eventsPage, PAGE_SIZE),
            getRecentAlerts(alertsPage, PAGE_SIZE),
            getOpenAlertsPaged(0, PAGE_SIZE),
            getAlertRules(),
          ]);

        setDevices(devicesData);

        setEvents(eventsData.content);
        setEventsPageData(eventsData);

        setAlerts(alertsData.content);
        setAlertsPageData(alertsData);

        setOpenAlerts(openAlertsData.content);
        setOpenAlertsPageData(openAlertsData);

        setRules(rulesData);

        setLastUpdatedAt(new Date());
      } catch (err) {
        setError(err instanceof Error ? err.message : "Unknown error loading dashboard");
      } finally {
        if (showLoading) {
          setLoading(false);
        }
      }
    },
    [eventsPage, alertsPage]
  );

  const loadDevicesData = useCallback(async () => {
    try {
      const devicesData = await getDevices();
      setDevices(devicesData);
      setLastUpdatedAt(new Date());
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unknown error loading devices");
    }
  }, []);

  const loadEventsData = useCallback(async () => {
    try {
      const eventsData = await getRecentEvents(eventsPage, PAGE_SIZE);
      setEvents(eventsData.content);
      setEventsPageData(eventsData);
      setLastUpdatedAt(new Date());
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unknown error loading events");
    }
  }, [eventsPage]);

  const loadAlertsData = useCallback(async () => {
    try {
      const [alertsData, openAlertsData] = await Promise.all([
        getRecentAlerts(alertsPage, PAGE_SIZE),
        getOpenAlertsPaged(0, PAGE_SIZE),
      ]);

      setAlerts(alertsData.content);
      setAlertsPageData(alertsData);

      setOpenAlerts(openAlertsData.content);
      setOpenAlertsPageData(openAlertsData);

      setLastUpdatedAt(new Date());
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unknown error loading alerts");
    }
  }, [alertsPage]);

  const loadRulesData = useCallback(async () => {
    try {
      const rulesData = await getAlertRules();
      setRules(rulesData);
      setLastUpdatedAt(new Date());
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unknown error loading rules");
    }
  }, []);

  const handleDashboardWebSocketMessage = useCallback(
    async (message: DashboardEventMessage) => {
      setLastWebSocketMessage(message);

      switch (message.type) {
        case "DEVICE_REGISTERED":
        case "DEVICE_HEARTBEAT_RECEIVED":
        case "DEVICE_STATUS_CHANGED":
          await loadDevicesData();
          break;

        case "SECURITY_EVENT_CREATED":
          await loadEventsData();
          break;

        case "ALERT_CREATED":
        case "ALERT_ACKNOWLEDGED":
        case "ALERT_RESOLVED":
          await loadAlertsData();
          break;

        case "RULE_CREATED":
        case "RULE_UPDATED":
        case "RULE_DELETED":
          await loadRulesData();
          break;

        default:
          await loadDashboardData(false);
          break;
      }
    },
    [
      loadDevicesData,
      loadEventsData,
      loadAlertsData,
      loadRulesData,
      loadDashboardData,
    ]
  );

  useEffect(() => {
    loadDashboardData(true);
  }, [loadDashboardData]);

  useEffect(() => {
    if (!autoRefreshEnabled) {
      return;
    }

    const intervalId = window.setInterval(() => {
      loadDashboardData(false);
    }, 5000);

    return () => {
      window.clearInterval(intervalId);
    };
  }, [autoRefreshEnabled, loadDashboardData]);

  useEffect(() => {
    const socket = createDashboardWebSocket(
      handleDashboardWebSocketMessage,
      () => {
        setWebSocketConnected(true);
      },
      () => {
        setWebSocketConnected(false);
      },
      () => {
        setWebSocketConnected(false);
      }
    );

    return () => {
      socket.close();
    };
  }, [handleDashboardWebSocketMessage]);

  const filteredDevices = useMemo(() => {
    const search = deviceSearch.trim().toLowerCase();

    return [...devices]
      .filter((device) => {
        const matchesSearch =
          search.length === 0 ||
          device.name.toLowerCase().includes(search) ||
          device.type.toLowerCase().includes(search) ||
          device.location.toLowerCase().includes(search);

        const matchesStatus =
          deviceStatusFilter === "ALL" || device.status === deviceStatusFilter;

        return matchesSearch && matchesStatus;
      })
      .sort((a, b) =>
        compareValues(
          getDeviceSortValue(a, deviceSortField),
          getDeviceSortValue(b, deviceSortField),
          deviceSortDirection
        )
      );
  }, [devices, deviceSearch, deviceStatusFilter, deviceSortField, deviceSortDirection]);

  const filteredEvents = useMemo(() => {
    const search = eventSearch.trim().toLowerCase();

    return [...events]
      .filter((event) => {
        const matchesSearch =
          search.length === 0 ||
          event.eventType.toLowerCase().includes(search) ||
          event.deviceName.toLowerCase().includes(search) ||
          event.severity.toLowerCase().includes(search);

        const matchesSeverity =
          eventSeverityFilter === "ALL" || event.severity === eventSeverityFilter;

        const matchesType =
          eventTypeFilter === "ALL" || event.eventType === eventTypeFilter;

        return matchesSearch && matchesSeverity && matchesType;
      })
      .sort((a, b) =>
        compareValues(
          getEventSortValue(a, eventSortField),
          getEventSortValue(b, eventSortField),
          eventSortDirection
        )
      );
  }, [
    events,
    eventSearch,
    eventSeverityFilter,
    eventTypeFilter,
    eventSortField,
    eventSortDirection,
  ]);

  const filteredAlerts = useMemo(() => {
    const search = alertSearch.trim().toLowerCase();

    return [...alerts]
      .filter((alert) => {
        const matchesSearch =
          search.length === 0 ||
          alert.title.toLowerCase().includes(search) ||
          alert.deviceName.toLowerCase().includes(search) ||
          alert.status.toLowerCase().includes(search) ||
          alert.severity.toLowerCase().includes(search);

        const matchesStatus =
          alertStatusFilter === "ALL" || alert.status === alertStatusFilter;

        const matchesSeverity =
          alertSeverityFilter === "ALL" || alert.severity === alertSeverityFilter;

        return matchesSearch && matchesStatus && matchesSeverity;
      })
      .sort((a, b) =>
        compareValues(
          getAlertSortValue(a, alertSortField),
          getAlertSortValue(b, alertSortField),
          alertSortDirection
        )
      );
  }, [
    alerts,
    alertSearch,
    alertStatusFilter,
    alertSeverityFilter,
    alertSortField,
    alertSortDirection,
  ]);

  const filteredRules = useMemo(() => {
    const search = ruleSearch.trim().toLowerCase();

    return [...rules]
      .filter((rule) => {
        const matchesSearch =
          search.length === 0 ||
          rule.name.toLowerCase().includes(search) ||
          (rule.description ?? "").toLowerCase().includes(search) ||
          (rule.eventType ?? "").toLowerCase().includes(search) ||
          (rule.deviceType ?? "").toLowerCase().includes(search) ||
          rule.alertSeverity.toLowerCase().includes(search);

        const matchesEnabled =
          ruleEnabledFilter === "ALL" ||
          (ruleEnabledFilter === "ENABLED" && rule.enabled) ||
          (ruleEnabledFilter === "DISABLED" && !rule.enabled);

        return matchesSearch && matchesEnabled;
      })
      .sort((a, b) =>
        compareValues(
          getRuleSortValue(a, ruleSortField),
          getRuleSortValue(b, ruleSortField),
          ruleSortDirection
        )
      );
  }, [rules, ruleSearch, ruleEnabledFilter, ruleSortField, ruleSortDirection]);

  async function handleAcknowledgeAlert(id: string) {
    await acknowledgeAlert(id);
    await loadAlertsData();
  }

  async function handleResolveAlert(id: string) {
    await resolveAlert(id);
    await loadAlertsData();
  }

  function resetRuleForm() {
    setRuleForm(defaultRuleForm);
    setEditingRuleId(null);
  }

  function startEditingRule(rule: AlertRule) {
    setEditingRuleId(rule.id);
    setRuleForm({
      name: rule.name,
      description: rule.description ?? "",
      enabled: rule.enabled,
      eventType: rule.eventType,
      minimumSeverity: rule.minimumSeverity,
      deviceType: rule.deviceType,
      alertSeverity: rule.alertSeverity,
      alertTitle: rule.alertTitle,
      alertMessage: rule.alertMessage,
    });
  }

  async function handleSubmitRule(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!ruleForm.name.trim()) {
      setError("Rule name is required");
      return;
    }

    if (!ruleForm.alertTitle.trim()) {
      setError("Alert title is required");
      return;
    }

    if (!ruleForm.alertMessage.trim()) {
      setError("Alert message is required");
      return;
    }

    try {
      setError(null);

      if (editingRuleId) {
        await updateAlertRule(editingRuleId, ruleForm);
      } else {
        await createAlertRule(ruleForm);
      }

      resetRuleForm();
      await loadRulesData();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unknown error saving rule");
    }
  }

  async function handleToggleRule(rule: AlertRule) {
    try {
      setError(null);

      if (rule.enabled) {
        await disableAlertRule(rule.id);
      } else {
        await enableAlertRule(rule.id);
      }

      await loadRulesData();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unknown error updating rule");
    }
  }

  async function handleDeleteRule(id: string) {
    try {
      setError(null);

      await deleteAlertRule(id);

      if (editingRuleId === id) {
        resetRuleForm();
      }

      await loadRulesData();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unknown error deleting rule");
    }
  }

  const onlineDevices = devices.filter((device) => device.status === "ONLINE").length;
  const offlineDevices = devices.filter((device) => device.status === "OFFLINE").length;

  const highSeverityEvents = events.filter(
    (event) => event.severity === "HIGH" || event.severity === "CRITICAL"
  ).length;

  return (
    <main className="app-shell">
      <header className="header">
        <div>
          <p className="eyebrow">SentinelMesh</p>
          <h1>Security Operations Dashboard</h1>
          <p className="subtitle">
            Monitor distributed devices, security events, alerts, and configurable rules.
          </p>
        </div>

        <div className="header-actions">
          <div className="last-updated">
            {lastUpdatedAt
              ? `Last updated: ${lastUpdatedAt.toLocaleTimeString()}`
              : "Not updated yet"}
          </div>

          <div className="websocket-status">
            WebSocket: {webSocketConnected ? "Connected" : "Disconnected"}
          </div>

          {lastWebSocketMessage && (
            <div className="last-updated">
              Last event: {lastWebSocketMessage.type}
            </div>
          )}

          <button
            className="refresh-button"
            onClick={() => setAutoRefreshEnabled((enabled) => !enabled)}
          >
            {autoRefreshEnabled ? "Pause Auto-Refresh" : "Resume Auto-Refresh"}
          </button>

          <button className="refresh-button" onClick={() => loadDashboardData(false)}>
            Refresh
          </button>
        </div>
      </header>

      {error && <div className="error-banner">{error}</div>}

      {loading ? (
        <div className="loading-card">Loading dashboard...</div>
      ) : (
        <>
          <section className="stats-grid">
            <StatCard label="Total Devices" value={devices.length} />
            <StatCard label="Online" value={onlineDevices} />
            <StatCard label="Offline" value={offlineDevices} />
            <StatCard label="Recent Events" value={events.length} />
            <StatCard label="Recent Alerts" value={alerts.length} />
            <StatCard label="Open Alerts" value={openAlerts.length} />
            <StatCard label="Rules" value={rules.length} />
            <StatCard label="High/Critical Events" value={highSeverityEvents} />
          </section>

          <section className="section">
            <div className="section-header">
              <h2>Open Alerts</h2>
              <p>Alerts that still need attention.</p>
            </div>

            <div className="table-card">
              <table>
                <thead>
                  <tr>
                    <th>Severity</th>
                    <th>Device</th>
                    <th>Title</th>
                    <th>Status</th>
                    <th>Created</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {openAlerts.length === 0 ? (
                    <tr>
                      <td colSpan={6}>No open alerts.</td>
                    </tr>
                  ) : (
                    openAlerts.map((alert) => (
                      <tr key={alert.id}>
                        <td>
                          <Badge value={alert.severity} />
                        </td>
                        <td>{alert.deviceName}</td>
                        <td>{alert.title}</td>
                        <td>
                          <Badge value={alert.status} />
                        </td>
                        <td>{formatDate(alert.createdAt)}</td>
                        <td className="actions">
                          <button onClick={() => handleAcknowledgeAlert(alert.id)}>
                            Acknowledge
                          </button>
                          <button onClick={() => handleResolveAlert(alert.id)}>
                            Resolve
                          </button>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>

              {openAlertsPageData && (
                <div className="pagination">
                  <span>
                    Showing {openAlerts.length} of {openAlertsPageData.totalElements} open alerts
                  </span>
                </div>
              )}
            </div>
          </section>

          <section className="two-column">
            <div className="section">
              <div className="section-header">
                <h2>Devices</h2>
                <p>Registered cameras and sensors.</p>
              </div>

              <div className="filter-bar">
                <input
                  value={deviceSearch}
                  onChange={(event) => setDeviceSearch(event.target.value)}
                  placeholder="Search devices..."
                />

                <select
                  value={deviceStatusFilter}
                  onChange={(event) =>
                    setDeviceStatusFilter(event.target.value as DeviceStatusFilter)
                  }
                >
                  <option value="ALL">All statuses</option>
                  <option value="ONLINE">Online</option>
                  <option value="OFFLINE">Offline</option>
                </select>

                <select
                  value={deviceSortField}
                  onChange={(event) =>
                    setDeviceSortField(event.target.value as DeviceSortField)
                  }
                >
                  <option value="name">Sort by name</option>
                  <option value="type">Sort by type</option>
                  <option value="location">Sort by location</option>
                  <option value="status">Sort by status</option>
                  <option value="lastSeenAt">Sort by last seen</option>
                </select>

                <SortDirectionButton
                  direction={deviceSortDirection}
                  onClick={() => setDeviceSortDirection(toggleSortDirection)}
                />

                <span className="filter-count">
                  Showing {filteredDevices.length} of {devices.length}
                </span>
              </div>

              <div className="table-card">
                <table>
                  <thead>
                    <tr>
                      <th>Name</th>
                      <th>Type</th>
                      <th>Location</th>
                      <th>Status</th>
                      <th>Last Seen</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredDevices.length === 0 ? (
                      <tr>
                        <td colSpan={5}>No devices match the current filters.</td>
                      </tr>
                    ) : (
                      filteredDevices.map((device) => (
                        <tr key={device.id}>
                          <td>{device.name}</td>
                          <td>{device.type}</td>
                          <td>{device.location}</td>
                          <td>
                            <Badge value={device.status} />
                          </td>
                          <td>
                            {device.lastSeenAt ? formatDate(device.lastSeenAt) : "Never"}
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>

            <div className="section">
              <div className="section-header">
                <h2>Recent Events</h2>
                <p>Latest paged security events from devices.</p>
              </div>

              <div className="filter-bar">
                <input
                  value={eventSearch}
                  onChange={(event) => setEventSearch(event.target.value)}
                  placeholder="Search events..."
                />

                <select
                  value={eventSeverityFilter}
                  onChange={(event) =>
                    setEventSeverityFilter(event.target.value as SeverityFilter)
                  }
                >
                  <option value="ALL">All severities</option>
                  <option value="LOW">LOW</option>
                  <option value="MEDIUM">MEDIUM</option>
                  <option value="HIGH">HIGH</option>
                  <option value="CRITICAL">CRITICAL</option>
                </select>

                <select
                  value={eventTypeFilter}
                  onChange={(event) =>
                    setEventTypeFilter(event.target.value as EventTypeFilter)
                  }
                >
                  <option value="ALL">All event types</option>
                  <option value="MOTION_DETECTED">MOTION_DETECTED</option>
                  <option value="PERSON_DETECTED">PERSON_DETECTED</option>
                  <option value="DOOR_OPENED">DOOR_OPENED</option>
                  <option value="SOUND_DETECTED">SOUND_DETECTED</option>
                </select>

                <select
                  value={eventSortField}
                  onChange={(event) =>
                    setEventSortField(event.target.value as EventSortField)
                  }
                >
                  <option value="receivedAt">Sort by received</option>
                  <option value="eventType">Sort by type</option>
                  <option value="severity">Sort by severity</option>
                  <option value="deviceName">Sort by device</option>
                  <option value="confidence">Sort by confidence</option>
                </select>

                <SortDirectionButton
                  direction={eventSortDirection}
                  onClick={() => setEventSortDirection(toggleSortDirection)}
                />

                <span className="filter-count">
                  Showing {filteredEvents.length} of {events.length}
                </span>
              </div>

              <div className="table-card">
                <table>
                  <thead>
                    <tr>
                      <th>Type</th>
                      <th>Severity</th>
                      <th>Device</th>
                      <th>Confidence</th>
                      <th>Received</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredEvents.length === 0 ? (
                      <tr>
                        <td colSpan={5}>No events match the current filters.</td>
                      </tr>
                    ) : (
                      filteredEvents.map((event) => (
                        <tr key={event.id}>
                          <td>{event.eventType}</td>
                          <td>
                            <Badge value={event.severity} />
                          </td>
                          <td>{event.deviceName}</td>
                          <td>
                            {event.confidence === null
                              ? "N/A"
                              : `${Math.round(event.confidence * 100)}%`}
                          </td>
                          <td>{formatDate(event.receivedAt)}</td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>

                {eventsPageData && (
                  <div className="pagination">
                    <button
                      disabled={!eventsPageData.hasPrevious}
                      onClick={() => setEventsPage((page) => Math.max(page - 1, 0))}
                    >
                      Previous
                    </button>

                    <span>
                      Page {eventsPageData.page + 1} of{" "}
                      {Math.max(eventsPageData.totalPages, 1)}
                    </span>

                    <button
                      disabled={!eventsPageData.hasNext}
                      onClick={() => setEventsPage((page) => page + 1)}
                    >
                      Next
                    </button>
                  </div>
                )}
              </div>
            </div>
          </section>

          <section className="section">
            <div className="section-header">
              <h2>Recent Alerts</h2>
              <p>Latest paged alerts across all statuses.</p>
            </div>

            <div className="filter-bar">
              <input
                value={alertSearch}
                onChange={(event) => setAlertSearch(event.target.value)}
                placeholder="Search alerts..."
              />

              <select
                value={alertStatusFilter}
                onChange={(event) =>
                  setAlertStatusFilter(event.target.value as AlertStatusFilter)
                }
              >
                <option value="ALL">All statuses</option>
                <option value="OPEN">OPEN</option>
                <option value="ACKNOWLEDGED">ACKNOWLEDGED</option>
                <option value="RESOLVED">RESOLVED</option>
              </select>

              <select
                value={alertSeverityFilter}
                onChange={(event) =>
                  setAlertSeverityFilter(event.target.value as SeverityFilter)
                }
              >
                <option value="ALL">All severities</option>
                <option value="LOW">LOW</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="HIGH">HIGH</option>
                <option value="CRITICAL">CRITICAL</option>
              </select>

              <select
                value={alertSortField}
                onChange={(event) =>
                  setAlertSortField(event.target.value as AlertSortField)
                }
              >
                <option value="createdAt">Sort by created</option>
                <option value="severity">Sort by severity</option>
                <option value="status">Sort by status</option>
                <option value="deviceName">Sort by device</option>
                <option value="title">Sort by title</option>
              </select>

              <SortDirectionButton
                direction={alertSortDirection}
                onClick={() => setAlertSortDirection(toggleSortDirection)}
              />

              <span className="filter-count">
                Showing {filteredAlerts.length} of {alerts.length}
              </span>
            </div>

            <div className="table-card">
              <table>
                <thead>
                  <tr>
                    <th>Severity</th>
                    <th>Status</th>
                    <th>Device</th>
                    <th>Title</th>
                    <th>Created</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredAlerts.length === 0 ? (
                    <tr>
                      <td colSpan={5}>No alerts match the current filters.</td>
                    </tr>
                  ) : (
                    filteredAlerts.map((alert) => (
                      <tr key={alert.id}>
                        <td>
                          <Badge value={alert.severity} />
                        </td>
                        <td>
                          <Badge value={alert.status} />
                        </td>
                        <td>{alert.deviceName}</td>
                        <td>{alert.title}</td>
                        <td>{formatDate(alert.createdAt)}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>

              {alertsPageData && (
                <div className="pagination">
                  <button
                    disabled={!alertsPageData.hasPrevious}
                    onClick={() => setAlertsPage((page) => Math.max(page - 1, 0))}
                  >
                    Previous
                  </button>

                  <span>
                    Page {alertsPageData.page + 1} of{" "}
                    {Math.max(alertsPageData.totalPages, 1)}
                  </span>

                  <button
                    disabled={!alertsPageData.hasNext}
                    onClick={() => setAlertsPage((page) => page + 1)}
                  >
                    Next
                  </button>
                </div>
              )}
            </div>
          </section>

          <section className="section">
            <div className="section-header">
              <h2>Alert Rules</h2>
              <p>Create and manage rules that turn security events into alerts.</p>
            </div>

            <div className="rules-layout">
              <form className="rule-form" onSubmit={handleSubmitRule}>
                <h3>{editingRuleId ? "Edit Rule" : "Create Rule"}</h3>

                <label>
                  Rule Name
                  <input
                    value={ruleForm.name}
                    onChange={(event) =>
                      setRuleForm((form) => ({ ...form, name: event.target.value }))
                    }
                    placeholder="High severity events"
                  />
                </label>

                <label>
                  Description
                  <textarea
                    value={ruleForm.description ?? ""}
                    onChange={(event) =>
                      setRuleForm((form) => ({
                        ...form,
                        description: event.target.value,
                      }))
                    }
                    placeholder="Creates alerts for HIGH and CRITICAL security events"
                  />
                </label>

                <label>
                  Event Type
                  <select
                    value={ruleForm.eventType ?? ""}
                    onChange={(event) =>
                      setRuleForm((form) => ({
                        ...form,
                        eventType: event.target.value
                          ? (event.target.value as AlertRuleEventType)
                          : null,
                      }))
                    }
                  >
                    <option value="">Any event type</option>
                    <option value="MOTION_DETECTED">MOTION_DETECTED</option>
                    <option value="PERSON_DETECTED">PERSON_DETECTED</option>
                    <option value="DOOR_OPENED">DOOR_OPENED</option>
                    <option value="SOUND_DETECTED">SOUND_DETECTED</option>
                  </select>
                </label>

                <label>
                  Minimum Severity
                  <select
                    value={ruleForm.minimumSeverity ?? ""}
                    onChange={(event) =>
                      setRuleForm((form) => ({
                        ...form,
                        minimumSeverity: event.target.value
                          ? (event.target.value as AlertRuleSeverity)
                          : null,
                      }))
                    }
                  >
                    <option value="">Any severity</option>
                    <option value="LOW">LOW</option>
                    <option value="MEDIUM">MEDIUM</option>
                    <option value="HIGH">HIGH</option>
                    <option value="CRITICAL">CRITICAL</option>
                  </select>
                </label>

                <label>
                  Device Type
                  <select
                    value={ruleForm.deviceType ?? ""}
                    onChange={(event) =>
                      setRuleForm((form) => ({
                        ...form,
                        deviceType: event.target.value
                          ? (event.target.value as AlertRuleDeviceType)
                          : null,
                      }))
                    }
                  >
                    <option value="">Any device type</option>
                    <option value="CAMERA">CAMERA</option>
                    <option value="MOTION_SENSOR">MOTION_SENSOR</option>
                    <option value="DOOR_SENSOR">DOOR_SENSOR</option>
                    <option value="SOUND_SENSOR">SOUND_SENSOR</option>
                  </select>
                </label>

                <label>
                  Alert Severity
                  <select
                    value={ruleForm.alertSeverity}
                    onChange={(event) =>
                      setRuleForm((form) => ({
                        ...form,
                        alertSeverity: event.target.value as AlertRuleSeverity,
                      }))
                    }
                  >
                    <option value="LOW">LOW</option>
                    <option value="MEDIUM">MEDIUM</option>
                    <option value="HIGH">HIGH</option>
                    <option value="CRITICAL">CRITICAL</option>
                  </select>
                </label>

                <label>
                  Alert Title
                  <input
                    value={ruleForm.alertTitle}
                    onChange={(event) =>
                      setRuleForm((form) => ({
                        ...form,
                        alertTitle: event.target.value,
                      }))
                    }
                    placeholder="{severity} security event: {eventType}"
                  />
                </label>

                <label>
                  Alert Message
                  <textarea
                    value={ruleForm.alertMessage}
                    onChange={(event) =>
                      setRuleForm((form) => ({
                        ...form,
                        alertMessage: event.target.value,
                      }))
                    }
                    placeholder="Device {deviceName} reported {eventType}."
                  />
                </label>

                <label className="checkbox-row">
                  <input
                    type="checkbox"
                    checked={ruleForm.enabled}
                    onChange={(event) =>
                      setRuleForm((form) => ({
                        ...form,
                        enabled: event.target.checked,
                      }))
                    }
                  />
                  Enabled
                </label>

                <div className="rule-form-actions">
                  <button type="submit">
                    {editingRuleId ? "Save Rule" : "Create Rule"}
                  </button>

                  {editingRuleId && (
                    <button type="button" onClick={resetRuleForm}>
                      Cancel
                    </button>
                  )}
                </div>
              </form>

              <div>
                <div className="filter-bar">
                  <input
                    value={ruleSearch}
                    onChange={(event) => setRuleSearch(event.target.value)}
                    placeholder="Search rules..."
                  />

                  <select
                    value={ruleEnabledFilter}
                    onChange={(event) =>
                      setRuleEnabledFilter(event.target.value as RuleEnabledFilter)
                    }
                  >
                    <option value="ALL">All rules</option>
                    <option value="ENABLED">Enabled</option>
                    <option value="DISABLED">Disabled</option>
                  </select>

                  <select
                    value={ruleSortField}
                    onChange={(event) =>
                      setRuleSortField(event.target.value as RuleSortField)
                    }
                  >
                    <option value="name">Sort by name</option>
                    <option value="enabled">Sort by enabled</option>
                    <option value="eventType">Sort by event type</option>
                    <option value="minimumSeverity">Sort by min severity</option>
                    <option value="deviceType">Sort by device type</option>
                    <option value="alertSeverity">Sort by alert severity</option>
                  </select>

                  <SortDirectionButton
                    direction={ruleSortDirection}
                    onClick={() => setRuleSortDirection(toggleSortDirection)}
                  />

                  <span className="filter-count">
                    Showing {filteredRules.length} of {rules.length}
                  </span>
                </div>

                <div className="table-card">
                  <table>
                    <thead>
                      <tr>
                        <th>Name</th>
                        <th>Enabled</th>
                        <th>Event Type</th>
                        <th>Min Severity</th>
                        <th>Device Type</th>
                        <th>Alert Severity</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredRules.length === 0 ? (
                        <tr>
                          <td colSpan={7}>No rules match the current filters.</td>
                        </tr>
                      ) : (
                        filteredRules.map((rule) => (
                          <tr key={rule.id}>
                            <td>{rule.name}</td>
                            <td>
                              <Badge value={rule.enabled ? "ENABLED" : "DISABLED"} />
                            </td>
                            <td>{rule.eventType ?? "Any"}</td>
                            <td>{rule.minimumSeverity ?? "Any"}</td>
                            <td>{rule.deviceType ?? "Any"}</td>
                            <td>
                              <Badge value={rule.alertSeverity} />
                            </td>
                            <td className="actions">
                              <button onClick={() => startEditingRule(rule)}>Edit</button>
                              <button onClick={() => handleToggleRule(rule)}>
                                {rule.enabled ? "Disable" : "Enable"}
                              </button>
                              <button onClick={() => handleDeleteRule(rule.id)}>
                                Delete
                              </button>
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </section>
        </>
      )}
    </main>
  );
}

function StatCard({ label, value }: { label: string; value: number }) {
  return (
    <div className="stat-card">
      <p>{label}</p>
      <strong>{value}</strong>
    </div>
  );
}

function Badge({ value }: { value: string }) {
  return <span className={`badge badge-${value.toLowerCase()}`}>{value}</span>;
}

function SortDirectionButton({
  direction,
  onClick,
}: {
  direction: SortDirection;
  onClick: () => void;
}) {
  return (
    <button className="sort-direction-button" type="button" onClick={onClick}>
      {direction === "asc" ? "Asc ↑" : "Desc ↓"}
    </button>
  );
}

function toggleSortDirection(previous: SortDirection): SortDirection {
  return previous === "asc" ? "desc" : "asc";
}

function compareValues(
  first: string | number | null,
  second: string | number | null,
  direction: SortDirection
) {
  const directionMultiplier = direction === "asc" ? 1 : -1;

  if (first === null && second === null) {
    return 0;
  }

  if (first === null) {
    return 1;
  }

  if (second === null) {
    return -1;
  }

  if (typeof first === "number" && typeof second === "number") {
    return (first - second) * directionMultiplier;
  }

  return String(first).localeCompare(String(second)) * directionMultiplier;
}

function getDeviceSortValue(device: Device, field: DeviceSortField) {
  switch (field) {
    case "name":
      return device.name;
    case "type":
      return device.type;
    case "location":
      return device.location;
    case "status":
      return device.status;
    case "lastSeenAt":
      return device.lastSeenAt ? Date.parse(device.lastSeenAt) : null;
    default:
      return device.name;
  }
}

function getEventSortValue(event: SecurityEvent, field: EventSortField) {
  switch (field) {
    case "receivedAt":
      return Date.parse(event.receivedAt);
    case "eventType":
      return event.eventType;
    case "severity":
      return severityRank[event.severity] ?? 0;
    case "deviceName":
      return event.deviceName;
    case "confidence":
      return event.confidence ?? null;
    default:
      return Date.parse(event.receivedAt);
  }
}

function getAlertSortValue(alert: Alert, field: AlertSortField) {
  switch (field) {
    case "createdAt":
      return Date.parse(alert.createdAt);
    case "severity":
      return severityRank[alert.severity] ?? 0;
    case "status":
      return alert.status;
    case "deviceName":
      return alert.deviceName;
    case "title":
      return alert.title;
    default:
      return Date.parse(alert.createdAt);
  }
}

function getRuleSortValue(rule: AlertRule, field: RuleSortField) {
  switch (field) {
    case "name":
      return rule.name;
    case "enabled":
      return rule.enabled ? 1 : 0;
    case "eventType":
      return rule.eventType ?? null;
    case "minimumSeverity":
      return rule.minimumSeverity ? severityRank[rule.minimumSeverity] ?? 0 : null;
    case "deviceType":
      return rule.deviceType ?? null;
    case "alertSeverity":
      return severityRank[rule.alertSeverity] ?? 0;
    default:
      return rule.name;
  }
}

function formatDate(value: string) {
  return new Date(value).toLocaleString();
}

export default App;