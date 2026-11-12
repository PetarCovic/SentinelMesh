import { getCameras } from "../api/sentinelMeshApi";
import { useApiQuery } from "../api/useApiQuery";
import CameraCard from "../components/CameraCard";

type CameraListPageProps = {
  onSelectCamera: (deviceId: string) => void;
};

export default function CameraListPage({ onSelectCamera }: CameraListPageProps) {
  const { data: cameras, loading, error, reload } = useApiQuery(getCameras);

  return (
    <section aria-labelledby="camera-list-heading">
      <div className="section-header">
        <h1 id="camera-list-heading">Cameras</h1>
        <p>View live feeds and saved recordings.</p>
      </div>

      {loading && <p className="loading-card" role="status">Loading cameras…</p>}

      {error && (
        <div className="error-banner">
          <p role="alert">Could not load cameras: {error}</p>
          <button type="button" className="refresh-button" onClick={reload}>
            Retry
          </button>
        </div>
      )}

      {cameras && (cameras.length === 0 ? (
        <p className="loading-card">No cameras found.</p>
      ) : (
        <div className="camera-grid">
          {cameras.map((camera) => (
            <CameraCard key={camera.deviceId} camera={camera} onSelect={onSelectCamera} />
          ))}
        </div>
      ))}
    </section>
  );
}