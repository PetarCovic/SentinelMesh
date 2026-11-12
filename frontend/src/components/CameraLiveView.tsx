import { useState } from "react";
import { buildApiUrl } from "../api/sentinelMeshApi";
import type { CameraSummary } from "../types";

export default function CameraLiveView({ camera }: { camera: CameraSummary }) {
  return (
    <section className="section" aria-labelledby="camera-live-heading">
      <div className="section-header">
        <h2 id="camera-live-heading">Live view</h2>
        <p>The live preview is video only.</p>
      </div>

      {camera.liveStatus === "LIVE" ? (
        <LiveImage key={camera.deviceId} camera={camera} />
      ) : (
        <p className="loading-card" role="status">
          {camera.liveStatus === "STALE"
            ? "Waiting for fresh frames from this camera."
            : "Live preview is unavailable. Check Playback for saved recordings."}
        </p>
      )}
    </section>
  );
}

function LiveImage({ camera }: { camera: CameraSummary }) {
  const [attempt, setAttempt] = useState(0);
  const [failed, setFailed] = useState(false);
  const url = buildApiUrl(
    `/api/devices/${encodeURIComponent(camera.deviceId)}/live?attempt=${attempt}`,
  );

  return (
    <>
      {failed ? (
        <p className="error-banner" role="alert">The live feed could not be loaded.</p>
      ) : (
        <img
          key={attempt}
          className="camera-media"
          src={url ?? undefined}
          alt={`Live view from ${camera.name}`}
          onError={() => setFailed(true)}
        />
      )}
      <button
        type="button"
        className="refresh-button"
        onClick={() => {
          setFailed(false);
          setAttempt((value) => value + 1);
        }}
      >
        Reconnect feed
      </button>
    </>
  );
}