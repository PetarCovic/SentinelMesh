import { useCallback, useState } from "react";
import { getCameraDetails } from "../api/sentinelMeshApi";
import { useApiQuery } from "../api/useApiQuery";
import CameraLiveView from "../components/CameraLiveView";
import RecentCameraEvents from "../components/RecentCameraEvents";
import RecordingTimeline from "../components/RecordingTimeline";

type CameraMode = "LIVE" | "PLAYBACK";

export default function CameraDetailsPage({ deviceId }: { deviceId: string }) {
  const [mode, setMode] = useState<CameraMode>("LIVE");
  const request = useCallback(
    (signal: AbortSignal) => getCameraDetails(deviceId, signal),
    [deviceId],
  );
  const { data: camera, loading, error, reload } = useApiQuery(
    request,
    mode === "LIVE" ? 5_000 : 0,
  );

  if (!camera) {
    return error ? (
      <div className="error-banner">
        <p role="alert">Could not load this camera: {error}</p>
        <button type="button" className="refresh-button" onClick={reload}>Retry</button>
      </div>
    ) : (
      <p className="loading-card" role="status">Loading camera…</p>
    );
  }

  return (
    <>
      <header className="section-header camera-details-header">
        <div>
          <h1>{camera.name}</h1>
          {camera.location && <p>{camera.location}</p>}
          <p>
            Device: <span className="camera-status">{camera.deviceStatus.toLowerCase()}</span>
            {" · "}
            Live feed: <span className="camera-status">{camera.liveStatus.toLowerCase()}</span>
          </p>
        </div>
        <button type="button" className="refresh-button" onClick={reload} disabled={loading}>
          Refresh camera
        </button>
      </header>

      {error && <p className="error-banner" role="alert">Could not refresh camera status: {error}</p>}

      <div className="camera-controls section" role="group" aria-label="Camera view">
        <button
          type="button"
          className="refresh-button"
          aria-pressed={mode === "LIVE"}
          onClick={() => setMode("LIVE")}
        >
          Live
        </button>
        <button
          type="button"
          className="refresh-button"
          aria-pressed={mode === "PLAYBACK"}
          onClick={() => setMode("PLAYBACK")}
        >
          Playback
        </button>
      </div>

      {mode === "LIVE" ? (
        <CameraLiveView camera={camera} />
      ) : camera.recordingAvailable ? (
        <RecordingTimeline camera={camera} />
      ) : (
        <section className="section">
          <h2>Recording timeline</h2>
          <p className="loading-card">No saved recordings for this camera.</p>
        </section>
      )}

      <RecentCameraEvents events={camera.recentEvents} />
    </>
  );
}