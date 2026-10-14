# SentinelMesh frontend

The frontend is a React and TypeScript dashboard built with Vite. It displays devices, security events, alerts, alert rules, dashboard updates, snapshots, and event clips.

## Prerequisites and run

Use Node.js 22.13+ (22.x) and npm, matching the Dockerfile and CI. Start a backend at `http://localhost:8080`.

From `frontend/`:

```powershell
npm ci
npm run dev
```

Open <http://localhost:5173>. Stop with `Ctrl+C`. If Vite selects another port because 5173 is occupied, free port 5173 or update the backend allowed origins before using that other port.

## Backend address configuration

The dashboard and media components use [src/api/sentinelMeshApi.ts](src/api/sentinelMeshApi.ts). It reads `VITE_API_BASE_URL` and defaults to `http://localhost:8080`.

Set the value before starting Vite:

```powershell
$env:VITE_API_BASE_URL = "http://localhost:8080"
npm run dev
```

That setting also controls the base for relative snapshot and clip URLs. Vite embeds it when building the static bundle; it is not a runtime setting for an already-built frontend container. The Compose Dockerfile currently exposes no build argument for it.

The dashboard WebSocket separately uses the hardcoded `ws://localhost:8080/ws/dashboard` constant in [src/api/dashboardWebSocket.ts](src/api/dashboardWebSocket.ts). Update it if the backend address changes. The older [src/api/client.ts](src/api/client.ts) also contains a fixed localhost base, but `App.tsx` uses `sentinelMeshApi.ts`.

The backend HTTP CORS and WebSocket settings allow origin `http://localhost:5173`; update those if the dashboard origin changes.

Dashboard messages use a native WebSocket with automatic reconnect. REST polling refreshes every five seconds when auto-refresh is enabled. Offline transitions currently rely on polling because the device monitor does not broadcast them.

## Checks

```powershell
npm ci
npm run test:run
npm run build
npm run lint
```

`test:run` executes Vitest once; `npm test` starts its watch mode. Build output is in `dist/`. CI currently runs tests and build; lint is a separate local check.

## Docker

From the repository root:

```powershell
docker compose up -d --build
```

The frontend image builds the bundle and serves it on container port 5173. Compose publishes the same port by default. It serves static files; the browser contacts the backend directly.

See [running locally](../docs/running-locally.md), [architecture](../docs/architecture.md), and the [runbook](../docs/runbook.md).
