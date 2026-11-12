import type { CameraSummary } from "../types";

type CameraCardProps = {
  camera: CameraSummary;
  onSelect: (deviceId: string) => void;
};

export default function CameraCard({ camera, onSelect }: CameraCardProps) {
  return (
    <article className="stat-card camera-card">
      <h3>{camera.name}</h3>
      {camera.location && <p>{camera.location}</p>}
      <p>
        Device: <span className="camera-status">{camera.deviceStatus.toLowerCase()}</span>
      </p>
      <p>
        Live feed: <span className="camera-status">{camera.liveStatus.toLowerCase()}</span>
      </p>
      <p>Recordings: {camera.recordingAvailable ? "Available" : "None available"}</p>
      <button
        type="button"
        className="refresh-button"
        aria-label={`View camera ${camera.name}`}
        onClick={() => onSelect(camera.deviceId)}
      >
        View camera
      </button>
    </article>
  );
}