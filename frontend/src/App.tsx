import { useCallback, useEffect, useState } from "react";
import {
  acknowledgeAlert,
  getDevices,
  getOpenAlertsPaged,
  getRecentAlerts,
  getRecentEvents,
  resolveAlert,
} from "./api/sentinelMeshApi";
import type { Alert, Device, PageResponse, SecurityEvent } from "./types";
import "./App.css";

const PAGE_SIZE = 50;

function App() {
  const [devices, setDevices] = useState<Device[]>([]);
  const [events, setEvents] = useState<SecurityEvent[]>([]);
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [openAlerts, setOpenAlerts] = useState<Alert[]>([]);

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

  const loadDashboardData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      const [devicesData, eventsData, alertsData, openAlertsData] =
        await Promise.all([
          getDevices(),
          getRecentEvents(eventsPage, PAGE_SIZE),
          getRecentAlerts(alertsPage, PAGE_SIZE),
          getOpenAlertsPaged(0, PAGE_SIZE),
        ]);

      setDevices(devicesData);

      setEvents(eventsData.content);
      setEventsPageData(eventsData);

      setAlerts(alertsData.content);
      setAlertsPageData(alertsData);

      setOpenAlerts(openAlertsData.content);
      setOpenAlertsPageData(openAlertsData);

      setLastUpdatedAt(new Date());
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unknown error loading dashboard");
    } finally {
      setLoading(false);
    }
  }, [eventsPage, alertsPage]);

  useEffect(() => {
    loadDashboardData();
  }, [loadDashboardData]);

  useEffect(() => {
    if (!autoRefreshEnabled) {
      return;
    }

    const intervalId = window.setInterval(() => {
      loadDashboardData();
    }, 5000);

    return () => {
      window.clearInterval(intervalId);
    };
  }, [autoRefreshEnabled, loadDashboardData]);

  async function handleAcknowledgeAlert(id: string) {
    await acknowledgeAlert(id);
    await loadDashboardData();
  }

  async function handleResolveAlert(id: string) {
    await resolveAlert(id);
    await loadDashboardData();
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
            Monitor distributed devices, security events, and active alerts.
          </p>
        </div>

        <div className="header-actions">
          <div className="last-updated">
            {lastUpdatedAt
              ? `Last updated: ${lastUpdatedAt.toLocaleTimeString()}`
              : "Not updated yet"}
          </div>

          <button
            className="refresh-button"
            onClick={() => setAutoRefreshEnabled((enabled) => !enabled)}
          >
            {autoRefreshEnabled ? "Pause Auto-Refresh" : "Resume Auto-Refresh"}
          </button>

          <button className="refresh-button" onClick={loadDashboardData}>
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
                    {devices.length === 0 ? (
                      <tr>
                        <td colSpan={5}>No devices registered.</td>
                      </tr>
                    ) : (
                      devices.map((device) => (
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
                    {events.length === 0 ? (
                      <tr>
                        <td colSpan={5}>No recent events.</td>
                      </tr>
                    ) : (
                      events.map((event) => (
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
                  {alerts.length === 0 ? (
                    <tr>
                      <td colSpan={5}>No recent alerts.</td>
                    </tr>
                  ) : (
                    alerts.map((alert) => (
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

function formatDate(value: string) {
  return new Date(value).toLocaleString();
}

export default App;