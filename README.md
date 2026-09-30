# SentinelMesh

SentinelMesh is a distributed smart-home security platform for collecting device heartbeats, processing security events, managing alerts, and reviewing camera media.

## Components

- **Backend** — Spring Boot REST API backed by PostgreSQL and Redis.
- **Frontend** — React and TypeScript operations dashboard with polling and dashboard WebSocket updates.
- **CV edge node** — Java/OpenCV camera client for motion detection, live frames, heartbeats, continuous recording, snapshots, and event clips.
- **Simulator and benchmarks** — Supporting modules for device and performance work.

## Quick start with Docker Compose

### Prerequisites

- Docker Desktop with Docker Compose
- A non-empty PostgreSQL password for local development

From the repository root:

```powershell
Copy-Item .env.example .env
notepad .env
```

Set `POSTGRES_PASSWORD` in `.env`. Keep `.env` local; it is ignored by Git.

Start the application:

```powershell
docker compose up --build
```

Open:

- Dashboard: <http://localhost:5173>
- API health: <http://localhost:8080/api/health>

Compose exposes PostgreSQL on host port `5433`; the backend reaches it inside Docker at `postgres:5432`. Redis is exposed on host port `6379`.

Stop the services with `Ctrl+C` when running in the foreground, or use:

```powershell
docker compose down
```

For the full local setup, hybrid development, tests, and troubleshooting, see [docs/running-locally.md](docs/running-locally.md).

## Running the edge node

The edge node requires Java 21, a working camera or video source, and a registered device API key. Set the key in the process environment before starting it:

```powershell
$env:SENTINEL_DEVICE_API_KEY = "your-device-api-key"
Set-Location cv_edge_node
mvn compile exec:java "-Dexec.mainClass=com.sentinelmesh.edge.CvEdgeNodeApplication"
```

Stop the foreground process with `Ctrl+C`. The default edge configuration targets the local camera and the configured SentinelMesh device; review `ConfigLoader` before connecting a different device.

## Tests and builds

Run the repository checks on Windows with:

```powershell
scripts\test-all.bat
```

Individual checks can be run from their module directories:

```powershell
backend\mvnw.cmd clean test
Set-Location frontend
npm install
npm run test:run
npm run build
```

## Configuration and local data

- Copy `.env.example` to `.env` and provide local values.
- `POSTGRES_PASSWORD` is required by Docker Compose and the backend Docker profile.
- `SENTINEL_DEVICE_API_KEY` is required by the edge node and must never be committed.
- Backend media is stored under the ignored `backend/storage/` directory.
- Edge recording spool files are stored under the ignored `cv_edge_node/recording-spool/` directory.

See [docs/database-environments.md](docs/database-environments.md) for database and Redis profile details.

## License

SentinelMesh is licensed under the [PolyForm Noncommercial License 1.0.0](LICENSE).
Commercial uses not permitted by that license require separate written permission from Petar Covic.