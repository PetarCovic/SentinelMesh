import { useState } from "react";
import { buildApiUrl } from "../api/sentinelMeshApi";
import type { RecordingSegment } from "../types";

export default function RecordingPlayer({ segment }: { segment: RecordingSegment }) {
  const [failed, setFailed] = useState(false);
  const url = buildApiUrl(segment.videoUrl);
  const start = new Date(segment.segmentStartTime).toLocaleString();

  if (!url) return <p role="alert">This recording has no playback URL.</p>;

  return (
    <div className="stat-card">
      <h3>Recording player</h3>
      <p>Starts {start}</p>
      {failed ? (
        <div className="error-banner">
          <p role="alert">This recording could not be loaded. It may no longer be available.</p>
          <button type="button" className="refresh-button" onClick={() => setFailed(false)}>
            Retry playback
          </button>
        </div>
      ) : (
        <video
          className="camera-media"
          aria-label={`Recording from ${start}`}
          src={url}
          controls
          playsInline
          preload="metadata"
          onError={() => setFailed(true)}
        />
      )}
      <a className="camera-link" href={url} target="_blank" rel="noreferrer">
        Open MP4 in a new tab
      </a>
    </div>
  );
}