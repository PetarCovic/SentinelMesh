# SentinelMesh frontend

The frontend is a React and TypeScript dashboard built with Vite. It displays devices, security events, alerts, alert rules, live dashboard updates, snapshots, and event clips from the SentinelMesh backend.

## Run locally

From `frontend/`:

```powershell
npm install
npm run dev
```

Open <http://localhost:5173>.

The API client defaults to `http://localhost:8080`. To point it at another backend, set `VITE_API_BASE_URL` before starting Vite:

```powershell
$env:VITE_API_BASE_URL = "http://localhost:8080"
npm run dev
```

The dashboard WebSocket uses `ws://localhost:8080/ws/dashboard` in the current implementation, so the backend must be reachable at that address for live updates.

## Checks

```powershell
npm run test:run
npm run build
npm run lint
```

## Docker

The root Docker Compose file builds the frontend and serves the production bundle on port `5173`:

```powershell
docker compose up --build frontend
```

For the complete stack, run `docker compose up --build` from the repository root. See [../docs/running-locally.md](../docs/running-locally.md).
