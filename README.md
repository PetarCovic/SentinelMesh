# SentinelMesh

SentinelMesh is a distributed smart-home security platform for collecting device heartbeats, processing security events, managing alerts, and reviewing camera media.

## Components and documentation

| Component | Implementation |
| --- | --- |
| Backend | Java 21, Spring Boot, PostgreSQL, Redis, REST APIs, and dashboard WebSocket updates |
| Frontend | React, TypeScript, and Vite dashboard |
| CV edge node | Java/OpenCV camera client with motion detection, optional person detection, live JPEG frames, snapshots, event clips, and continuous recording |
| Simulator and benchmarks | Separate Maven modules for synthetic devices and load testing |

| Document | Contents |
| --- | --- |
| [Requirements](docs/requirements.md) | Core behaviors, acceptance criteria, and verification coverage |
| [Architecture](docs/architecture.md) | Components, data flow, API contracts, and database relationships |
| [Runbook](docs/runbook.md) | Startup, shutdown, logs, backups, and troubleshooting |
| [Running locally](docs/running-locally.md) | Docker and host development instructions |
| [Database environments](docs/database-environments.md) | Database profiles, ports, queue names, and credential handling |
| [Frontend](frontend/README.md) | Dashboard development and current backend URL configuration |

## Prerequisites

- Git to clone the repository.
- Docker Desktop with Docker Compose v2 for the full stack.
- Java **21** for host-side Java development.
- Node.js **22.13+ (22.x)** and npm for host-side frontend development; this matches the Dockerfile and CI.
- Maven **3.9+** for the camera edge run command. Backend builds use the included Maven wrapper.
- A webcam or readable video file when running the CV edge node.

Docker builds supply Java and Node inside their build containers. The camera edge node runs separately on the host.

## Clone and configure

The commands below use **Windows PowerShell**. Run them from the repository root unless a block changes directory.

```powershell
git clone https://github.com/PetarCovic/SentinelMesh.git
Set-Location SentinelMesh
Copy-Item .env.example .env
notepad .env
```

For an existing checkout, create `.env` only if it is missing; do not overwrite your saved credentials. Set a non-empty `POSTGRES_PASSWORD` in that file.

| Setting | Purpose |
| --- | --- |
| `POSTGRES_DB` | Compose database; defaults to `sentinelmesh_dev` |
| `POSTGRES_USER` | Compose database role; defaults to `postgres` |
| `POSTGRES_PASSWORD` | Required local database password; no committed fallback |
| `BACKEND_PORT` | Published backend port; defaults to `8080` |
| `FRONTEND_PORT` | Published frontend port; defaults to `5173` |
| `SENTINEL_DEVICE_API_KEY` | Process environment variable required by the camera edge node |

Keep `.env` and device API keys local. Docker Compose reads the root `.env`; a backend or edge process started from PowerShell or Eclipse does **not** automatically load it.

The dashboard REST/media client reads `VITE_API_BASE_URL` and defaults to `http://localhost:8080`. Its WebSocket URL is still hardcoded to `ws://localhost:8080/ws/dashboard`. Keep default ports for the complete supplied setup; a different backend address needs both the REST setting and the WebSocket URL updated. A different dashboard origin also needs backend HTTP/WebSocket allowed-origin changes.

## Database setup and full-stack run

```powershell
docker compose config --quiet
docker compose up -d --build
docker compose ps
```

Compose starts PostgreSQL 18, Redis 7, the backend, and the frontend. On an **empty** PostgreSQL volume, it creates the role and database from your environment settings. The backend creates or updates application tables through Hibernate `ddl-auto: update`; there is no separate migration command in the current repository.

| Service | Host address | Address used by the backend container |
| --- | --- | --- |
| Dashboard | `http://localhost:5173` | Browser connects to the published backend |
| Backend | `http://localhost:8080` | Backend listens on container port `8080` |
| PostgreSQL | `localhost:5433` | `postgres:5432` |
| Redis | `localhost:6379` | `redis:6379` |

PostgreSQL and Redis use named volumes. Media uses the host bind mount `backend/storage/`. Changing `POSTGRES_PASSWORD` in `.env` does not change an existing PostgreSQL role password; see the [runbook](docs/runbook.md#database-connection-failures).

Verify the API and a database-backed route:

```powershell
$base = "http://localhost:8080"
Invoke-RestMethod "$base/api/health"
Invoke-RestMethod "$base/api/devices"
```

The dashboard is at <http://localhost:5173>. If Windows routes `localhost` incorrectly, see the runbook's [IPv6 check](docs/runbook.md#localhost-or-ipv6-routing).

Stop the stack without removing its volumes:

```powershell
docker compose down
```

## Run the backend and frontend from source

This setup uses Compose infrastructure and host processes. Stop any containerized backend/frontend first so their ports are free:

```powershell
docker compose stop backend frontend
docker compose up -d postgres redis
```

Read `POSTGRES_PASSWORD` from your local secret store into the backend process environment. In the following command, `Read-Host` prompts for it without displaying it:

```powershell
$credential = [pscredential]::new("postgres", (Read-Host "PostgreSQL password" -AsSecureString))
$env:POSTGRES_PASSWORD = $credential.GetNetworkCredential().Password
Remove-Variable credential
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5433/sentinelmesh_dev"
$env:SPRING_DATASOURCE_USERNAME = "postgres"
Push-Location backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Use your configured database and role if they differ from the defaults. The datasource override is necessary because `application-dev.yml` normally points at host port `5432`. Stop with `Ctrl+C`, then use `Pop-Location`.

In another terminal, from the repository root:

```powershell
Push-Location frontend
npm ci
npm run dev
```

Stop with `Ctrl+C`, then use `Pop-Location`. More profile options are in [database-environments.md](docs/database-environments.md).

## Build and test

Backend tests and a runnable JAR. Select the test profile explicitly and clear host datasource overrides so these checks use H2:

```powershell
$env:SPRING_PROFILES_ACTIVE = "test"
Remove-Item Env:SPRING_DATASOURCE_URL, Env:SPRING_DATASOURCE_USERNAME, Env:SPRING_DATASOURCE_PASSWORD -ErrorAction SilentlyContinue
Push-Location backend
.\mvnw.cmd --batch-mode --no-transfer-progress clean verify
Pop-Location
```

The JAR is `backend/target/backend-0.0.1-SNAPSHOT.jar`. Reapply the host datasource/password configuration shown above before running it from `backend/`, because the test setup clears runtime datasource overrides:

```powershell
Push-Location backend
java -jar target/backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

Stop with `Ctrl+C`, then use `Pop-Location`.

Frontend tests, production build, and lint:

```powershell
Push-Location frontend
npm ci
npm run test:run
npm run build
npm run lint
Pop-Location
```

Other Java modules, using the repository wrapper:

```powershell
Push-Location backend
.\mvnw.cmd --file ../cv_edge_node/pom.xml compile
.\mvnw.cmd --file ../simulator/pom.xml test
.\mvnw.cmd --file ../benchmarks/pom.xml compile
Pop-Location
```

With `SPRING_PROFILES_ACTIVE=test` and no runtime datasource overrides, the backend test suite uses H2 and controlled queue substitutes and does not require a running PostgreSQL/Redis stack. These tests do not establish PostgreSQL-specific behavior or camera/native-media behavior.

After `npm ci`, the Windows helper runs backend tests followed by frontend tests and build:

```powershell
$env:SPRING_PROFILES_ACTIVE = "test"
Remove-Item Env:SPRING_DATASOURCE_URL, Env:SPRING_DATASOURCE_USERNAME, Env:SPRING_DATASOURCE_PASSWORD -ErrorAction SilentlyContinue
.\scripts\test-all.bat
```

That helper does not compile the edge module or run lint. CI separately verifies backend packaging, frontend tests/build, and edge compilation; see [.github/workflows/ci.yml](.github/workflows/ci.yml).

## Run the camera edge node

1. Register a `CAMERA` using `POST /api/devices`; the response returns its API key once.
2. Edit `SENTINEL_DEVICE_ID` in [ConfigLoader.java](cv_edge_node/src/main/java/com/sentinelmesh/edge/config/ConfigLoader.java) to the new device's UUID.
3. Review the backend URL, camera index/video-file path, model path, and detection flags in that same class. Person detection is disabled by default.
4. Set the API key in the terminal that starts the edge process, or in the Eclipse launch configuration's environment.

For example, after registration has produced a local `$camera` response:

```powershell
$env:SENTINEL_DEVICE_API_KEY = $camera.apiKey
Push-Location cv_edge_node
mvn compile exec:java "-Dexec.mainClass=com.sentinelmesh.edge.CvEdgeNodeApplication"
```

The quoted `-D` argument is intentional for PowerShell. Stop with `Ctrl+C`, allow recording shutdown to finish, then use `Pop-Location`. A newly opened terminal may require setting the key again.

Registration, heartbeat, event, and alert verification commands are in the [runbook](docs/runbook.md#functional-smoke-test).

## Local data and current boundaries

- PostgreSQL stores device, event, alert, rule, and media metadata.
- Backend media files are in the ignored `backend/storage/` directory.
- Edge recording spool files are in the ignored `cv_edge_node/recording-spool/` directory.
- Live preview is an in-memory JPEG/MJPEG path; restarting the backend clears the latest live frames.
- The repository currently implements device-key checks for heartbeats, event ingestion, and snapshot upload. Other routes have uneven authentication coverage; user authentication and production access control are still work in progress.
- Automatic backend retention/reconciliation is not wired into the current published code. Preserve storage and database backups together.

See [SECURITY.md](SECURITY.md) for vulnerability reporting.

## License

SentinelMesh is licensed under the [PolyForm Noncommercial License 1.0.0](LICENSE).
Commercial uses not permitted by that license require separate written permission from Petar Covic.
