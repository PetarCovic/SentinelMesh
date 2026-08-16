import { useState } from "react";
import { buildApiUrl } from "../api/sentinelMeshApi";

interface EventSnapshotThumbnailProps {
  snapshotImageUrl: string | null;
  eventType: string;
  deviceName: string;
}

export default function EventSnapshotThumbnail({
  snapshotImageUrl,
  eventType,
  deviceName,
}: EventSnapshotThumbnailProps) {
  const [isPreviewOpen, setIsPreviewOpen] = useState(false);

  const imageUrl = buildApiUrl(snapshotImageUrl);

  if (!imageUrl) {
    return (
      <div className="snapshot-placeholder">
        <span>No snapshot</span>
      </div>
    );
  }

  return (
    <>
      <button
        className="snapshot-thumbnail-button"
        type="button"
        onClick={() => setIsPreviewOpen(true)}
        aria-label={`Open snapshot for ${eventType} event from ${deviceName}`}
      >
        <img
          className="snapshot-thumbnail"
          src={imageUrl}
          alt={`Snapshot for ${eventType} event from ${deviceName}`}
          loading="lazy"
        />
      </button>

      {isPreviewOpen && (
        <div
          className="snapshot-modal-backdrop"
          role="button"
          tabIndex={0}
          onClick={() => setIsPreviewOpen(false)}
          onKeyDown={(event) => {
            if (event.key === "Escape" || event.key === "Enter") {
              setIsPreviewOpen(false);
            }
          }}
        >
          <div
            className="snapshot-modal-content"
            role="dialog"
            aria-modal="true"
            aria-label="Snapshot preview"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="snapshot-modal-header">
              <h3>Event Snapshot</h3>
              <button
                type="button"
                className="snapshot-modal-close"
                onClick={() => setIsPreviewOpen(false)}
              >
                ×
              </button>
            </div>

            <img
              className="snapshot-modal-image"
              src={imageUrl}
              alt={`Snapshot preview for ${eventType} event from ${deviceName}`}
            />
          </div>
        </div>
      )}
    </>
  );
}