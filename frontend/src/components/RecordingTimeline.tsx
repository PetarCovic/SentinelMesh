import { useCallback, useState, type FormEvent } from "react";
import { getCameraRecordingTimeline } from "../api/sentinelMeshApi";
import { useApiQuery } from "../api/useApiQuery";
import type { CameraRecordingTimeline, CameraSummary, RecordingSegment } from "../types";
import RecordingPlayer from "./RecordingPlayer";

const MINUTE_MS = 60_000;
const HOUR_MS = 60 * MINUTE_MS;
const MAX_RANGE_MS = 7 * 24 * HOUR_MS;
const PAGE_SIZE = 25;

function defaultQuery(latestRecordingTime: string | null) {
  const latest = latestRecordingTime ? Date.parse(latestRecordingTime) : Date.now();
  const endMs = Math.ceil(latest / MINUTE_MS) * MINUTE_MS;
  return {
    start: new Date(endMs - HOUR_MS).toISOString(),
    end: new Date(endMs).toISOString(),
    page: 0,
  };
}

function toLocalInput(iso: string) {
  const date = new Date(iso);
  const localMs = date.getTime() - date.getTimezoneOffset() * MINUTE_MS;
  return new Date(localMs).toISOString().slice(0, 16);
}

function formatTime(iso: string) {
  return new Date(iso).toLocaleString();
}

export default function RecordingTimeline({ camera }: { camera: CameraSummary }) {
  const { deviceId, earliestRecordingTime, latestRecordingTime } = camera;
  const [query, setQuery] = useState(() => defaultQuery(latestRecordingTime));
  const [startInput, setStartInput] = useState(() => toLocalInput(query.start));
  const [endInput, setEndInput] = useState(() => toLocalInput(query.end));
  const [rangeError, setRangeError] = useState<string | null>(null);
  const [selected, setSelected] = useState<RecordingSegment | null>(null);

  const request = useCallback(
    (signal: AbortSignal) => getCameraRecordingTimeline(
      deviceId, query.start, query.end, query.page, PAGE_SIZE, signal,
    ),
    [deviceId, query],
  );
  const { data: timeline, loading, error, reload } = useApiQuery(request);

  function search(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const startMs = Date.parse(startInput);
    const endMs = Date.parse(endInput);
    if (!Number.isFinite(startMs) || !Number.isFinite(endMs) || endMs <= startMs) {
      setRangeError("Enter a start and an end time. End must be after start.");
      return;
    }
    if (endMs - startMs > MAX_RANGE_MS) {
      setRangeError("Search a range of seven days or less.");
      return;
    }

    setRangeError(null);
    setSelected(null);
    setQuery({
      start: new Date(startMs).toISOString(),
      end: new Date(endMs).toISOString(),
      page: 0,
    });
  }

  const page = timeline?.segments;

  return (
    <section className="section" aria-labelledby="recording-timeline-heading">
      <div className="section-header">
        <h2 id="recording-timeline-heading">Recording timeline</h2>
        {earliestRecordingTime && latestRecordingTime && (
          <p>Saved range: {formatTime(earliestRecordingTime)} to {formatTime(latestRecordingTime)}</p>
        )}
      </div>

      <form className="camera-controls recording-search" onSubmit={search}>
        <label>
          Start (local time)
          <input
            type="datetime-local"
            required
            value={startInput}
            onChange={(event) => setStartInput(event.target.value)}
          />
        </label>
        <label>
          End (local time)
          <input
            type="datetime-local"
            required
            value={endInput}
            onChange={(event) => setEndInput(event.target.value)}
          />
        </label>
        <button type="submit" className="refresh-button">Search recordings</button>
      </form>

      {rangeError && <p className="error-banner" role="alert">{rangeError}</p>}
      {loading && <p className="loading-card" role="status">Loading recordings…</p>}
      {error && (
        <div className="error-banner">
          <p role="alert">Could not load recordings: {error}</p>
          <button type="button" className="refresh-button" onClick={reload}>Retry</button>
        </div>
      )}

      {timeline && page && (page.totalElements === 0 ? (
        <p className="loading-card">No recordings in this time range.</p>
      ) : (
        <RecordingCoverage timeline={timeline} />
      ))}

      {selected && <RecordingPlayer key={selected.id} segment={selected} />}

      {page && page.totalElements > 0 && (
        <>
          <ul className="recording-segments">
            {page.content.map((segment) => (
              <li key={segment.id}>
                <button
                  type="button"
                  className="refresh-button recording-segment"
                  aria-pressed={selected?.id === segment.id}
                  onClick={() => setSelected(segment)}
                >
                  <span>{formatTime(segment.segmentStartTime)} – {formatTime(segment.segmentEndTime)}</span>
                  <span>{segment.durationSeconds} s</span>
                </button>
              </li>
            ))}
          </ul>
          {page.content.length === 0 && <p>No segments on this page.</p>}
          {page.totalPages > 1 && (
            <div className="pagination" aria-label="Recording pages">
              <button
                type="button"
                disabled={!page.hasPrevious}
                onClick={() => setQuery({ ...query, page: page.page - 1 })}
              >
                Previous
              </button>
              <span>Page {page.page + 1} of {page.totalPages}</span>
              <button
                type="button"
                disabled={!page.hasNext}
                onClick={() => setQuery({ ...query, page: page.page + 1 })}
              >
                Next
              </button>
            </div>
          )}
        </>
      )}
    </section>
  );
}

function RecordingCoverage({ timeline }: { timeline: CameraRecordingTimeline }) {
  const startMs = Date.parse(timeline.requestedStart);
  const durationMs = Date.parse(timeline.requestedEnd) - startMs;
  const position = (iso: string) => Math.max(0, Math.min(100, (
    (Date.parse(iso) - startMs) / durationMs
  ) * 100));

  return (
    <div className="section">
      <p>{timeline.segments.totalElements} segments in this range</p>
      <div className="recording-coverage" aria-hidden="true">
        {timeline.gaps.map((gap) => {
          const left = position(gap.gapStart);
          const width = position(gap.gapEnd) - left;
          return <span key={gap.gapStart} style={{ left: `${left}%`, width: `${width}%` }} />;
        })}
      </div>
      <p className="subtitle">Green: recorded · Gray: no recording</p>
      {timeline.gaps.length > 0 && (
        <details>
          <summary>{timeline.gaps.length} gaps in this range</summary>
          <ul>
            {timeline.gaps.slice(0, 20).map((gap) => (
              <li key={gap.gapStart}>
                {formatTime(gap.gapStart)} – {formatTime(gap.gapEnd)}
              </li>
            ))}
          </ul>
          {timeline.gaps.length > 20 && <p>Narrow the time range to see more gap details.</p>}
        </details>
      )}
    </div>
  );
}