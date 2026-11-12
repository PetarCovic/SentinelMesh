import type { SecurityEvent } from "../types";
import EventSnapshotThumbnail from "./EventSnapshotThumbnail";
import EventVideoClipButton from "./EventVideoClipButton";

export default function RecentCameraEvents({ events }: { events: SecurityEvent[] }) {
  return (
    <section className="section" aria-labelledby="camera-events-heading">
      <div className="section-header">
        <h2 id="camera-events-heading">Recent events</h2>
      </div>

      {events.length === 0 ? <p>No recent events for this camera.</p> : (
        <div className="table-card">
          <table>
            <thead>
              <tr>
                <th scope="col">Time</th>
                <th scope="col">Event</th>
                <th scope="col">Severity</th>
                <th scope="col">Snapshot</th>
                <th scope="col">Clip</th>
              </tr>
            </thead>
            <tbody>
              {events.map((event) => (
                <tr key={event.id}>
                  <td>
                    <time dateTime={event.occurredAt}>
                      {new Date(event.occurredAt).toLocaleString()}
                    </time>
                  </td>
                  <td>{event.eventType.replaceAll("_", " ")}</td>
                  <td>
                    <span className={`badge badge-${event.severity.toLowerCase()}`}>
                      {event.severity}
                    </span>
                  </td>
                  <td>
                    <EventSnapshotThumbnail
                      snapshotImageUrl={event.snapshotAvailable ? event.snapshotImageUrl : null}
                      eventType={event.eventType}
                      deviceName={event.deviceName}
                    />
                  </td>
                  <td>
                    <EventVideoClipButton
                      videoClipAvailable={event.videoClipAvailable}
                      videoClipUrl={event.videoClipUrl}
                      eventType={event.eventType}
                      deviceName={event.deviceName}
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}