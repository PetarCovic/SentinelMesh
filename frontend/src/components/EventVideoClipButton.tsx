import { useState } from "react";
import { buildApiUrl } from "../api/sentinelMeshApi";

type EventVideoClipButtonProps = {
  videoClipAvailable: boolean;
  videoClipUrl: string | null;
  eventType: string;
  deviceName: string;
};

export default function EventVideoClipButton({
  videoClipAvailable,
  videoClipUrl,
  eventType,
  deviceName,
}: EventVideoClipButtonProps) {
  const [isOpen, setIsOpen] = useState(false);

  const resolvedVideoUrl = buildApiUrl(videoClipUrl);

  if (!videoClipAvailable || !resolvedVideoUrl) {
    return <span className="video-clip-placeholder">No clip</span>;
  }

  return (
    <>
      <button
        type="button"
        className="video-clip-button"
        onClick={() => setIsOpen(true)}
      >
        View clip
      </button>

      {isOpen && (
        <div
          className="video-clip-modal-backdrop"
          role="dialog"
          aria-modal="true"
          aria-label="Event video clip"
          onClick={() => setIsOpen(false)}
        >
          <div
            className="video-clip-modal"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="video-clip-modal-header">
              <div>
                <h2>Event Clip</h2>
                <p>
                  {deviceName} · {eventType}
                </p>
              </div>

              <button
                type="button"
                className="video-clip-close-button"
                onClick={() => setIsOpen(false)}
                aria-label="Close video clip"
              >
                ×
              </button>
            </div>

            <video
              className="video-clip-player"
              src={resolvedVideoUrl}
              controls
              preload="metadata"
            >
              Your browser does not support the video tag.
            </video>

            <a
              className="video-clip-open-link"
              href={resolvedVideoUrl}
              target="_blank"
              rel="noreferrer"
            >
              Open video in new tab
            </a>
          </div>
        </div>
      )}
    </>
  );
}