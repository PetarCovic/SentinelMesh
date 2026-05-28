import { useEffect, useState } from "react";
import {
  acknowledgeAlert,
  getAlerts,
  getDevices,
  getEvents,
  getOpenAlerts,
  resolveAlert,
} from "./api/sentinelMeshApi";
import type { Alert, Device, SecurityEvent } from "./types";
import "./App.css";

function App() {
  const [devices, setDevices] = useState<Device[]>([]);
  const [events, setEvents] = useState<SecurityEvent[]>([]);
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [openAlerts, setOpenAlerts] = useState<Alert[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  async function loadDashboardData() {
    try {
      setLoading(true);
      setError(null);

      const [devicesData, eventsData, alertsData, openAlertsData] =
        await Promise.all([
          getDevices(),
          getEvents(),
          getAlerts(),
          getOpenAlerts(),
        ]);

      setDevices(devicesData);
      setEvents(eventsData);
      setAlerts(alertsData);
      setOpenAlerts(openAlertsData);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unknown error loading dashboard");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadDashboardData();
  }, []);

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

        <button className="refresh-button" onClick={loadDashboardData}>
          Refresh
        </button>
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
            <StatCard label="Security Events" value={events.length} />
            <StatCard label="Total Alerts" value={alerts.length} />
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
                        <td>{alert.status}</td>
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
                    {devices.map((device) => (
                      <tr key={device.id}>
                        <td>{device.name}</td>
                        <td>{device.type}</td>
                        <td>{device.location}</td>
                        <td>
                          <Badge value={device.status} />
                        </td>
                        <td>{device.lastSeenAt ? formatDate(device.lastSeenAt) : "Never"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            <div className="section">
              <div className="section-header">
                <h2>Recent Events</h2>
                <p>Latest security events from devices.</p>
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
                    {events.slice(-10).reverse().map((event) => (
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
                    ))}
                  </tbody>
                </table>
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

function formatDate(value: string) {
  return new Date(value).toLocaleString();
}

export default App;