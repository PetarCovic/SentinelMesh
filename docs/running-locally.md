# Running SentinelMesh Locally

SentinelMesh supports a full Docker Compose run and a hybrid development run.

## Prerequisites

- Docker Desktop with Docker Compose
- Java 21 for backend and edge-node development
- Node.js 22.13+ (22.x) and npm for frontend development
- A non-empty local PostgreSQL password

## Full Docker Compose mode

This starts PostgreSQL, Redis, the Spring Boot backend, and the React frontend.

From the repository root:

```powershell
Copy-Item .env.example .env
notepad .env
```

Set `POSTGRES_PASSWORD` in `.env`, then start the services:

```powershell
docker compose up --build
```

URLs:

- Frontend: <http://localhost:5173>
- Backend health: <http://localhost:8080/api/health>
- PostgreSQL from the host: `localhost:5433`
- Redis from the host: `localhost:6379`

Inside the backend container, PostgreSQL is reached as `postgres:5432` and Redis as `redis:6379`.

Stop the foreground process with `Ctrl+C`. To stop detached services:

```powershell
docker compose down
```

To remove the Docker PostgreSQL and Redis volumes as well:

```powershell
scripts\reset-docker.bat
```

The reset script asks for confirmation and permanently removes the Docker-managed database and Redis data.

## Hybrid development mode

Hybrid mode runs infrastructure in Docker and the backend and frontend from their source directories.

The current `dev` profile expects PostgreSQL on host port `5432`, while Compose publishes it on `5433`. Use the explicit datasource override below when using Compose infrastructure.

From the repository root, stop any containerized backend/frontend first, then start infrastructure:

```powershell
docker compose stop backend frontend
docker compose up -d postgres redis
```

Prompt for the password without displaying it, and set the host backend configuration:

```powershell
$credential = [pscredential]::new("postgres", (Read-Host "PostgreSQL password" -AsSecureString))
$env:POSTGRES_PASSWORD = $credential.GetNetworkCredential().Password
Remove-Variable credential
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5433/sentinelmesh_dev"
$env:SPRING_DATASOURCE_USERNAME = "postgres"
Set-Location backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Use your configured database/role if they differ from the defaults. Docker Compose reads the root `.env`; the host backend does not load it automatically. Stop the backend with `Ctrl+C`.

In another terminal, start the frontend:

```powershell
Set-Location frontend
npm ci
npm run dev
```

The frontend runs at <http://localhost:5173> and the backend at <http://localhost:8080>.

## Tests

Run all backend and frontend checks from the repository root:

```powershell
$env:SPRING_PROFILES_ACTIVE = "test"
Remove-Item Env:SPRING_DATASOURCE_URL, Env:SPRING_DATASOURCE_USERNAME, Env:SPRING_DATASOURCE_PASSWORD -ErrorAction SilentlyContinue
.\scripts\test-all.bat
```

Run only backend tests:

```powershell
$env:SPRING_PROFILES_ACTIVE = "test"
Remove-Item Env:SPRING_DATASOURCE_URL, Env:SPRING_DATASOURCE_USERNAME, Env:SPRING_DATASOURCE_PASSWORD -ErrorAction SilentlyContinue
Set-Location backend
.\mvnw.cmd clean test
```

Run frontend tests and the production build:

```powershell
Set-Location frontend
npm ci
npm run test:run
npm run build
```

## Manual API smoke test

Create a temporary camera device:

```powershell
$body = @{ name = "Local Test Camera"; type = "CAMERA"; location = "Lab" } |
    ConvertTo-Json -Compress
$camera = Invoke-RestMethod "http://localhost:8080/api/devices" -Method Post `
    -ContentType "application/json" -Body $body
$camera | Select-Object id, name, status
```

The response contains a device API key. Save it only in a local secret store or environment variable if you need to test a heartbeat. Do not commit it.

## Common issues

### The backend cannot connect to PostgreSQL

Inside Docker, use `postgres:5432`. From a process running on the host, use the host port configured for your PostgreSQL instance. Compose publishes PostgreSQL on `localhost:5433`.

### Compose refuses to start

Make sure `.env` exists and contains a non-empty `POSTGRES_PASSWORD`:

```powershell
Test-Path .env
Select-String -Path .env -Pattern '^POSTGRES_PASSWORD=.+$' -Quiet
```

### The health URL returns 404

Use `/api/health`. The root URL `/` does not serve a web page from the backend.

### The edge node stops immediately

Check that `SENTINEL_DEVICE_API_KEY` is set in the same terminal that starts the Java process, and stop or restart a foreground run with `Ctrl+C`.

## Useful commands

```powershell
docker compose ps
docker compose logs -f backend
docker compose logs -f frontend
docker compose down
```

## Related documentation

See the [requirements](requirements.md), [architecture](architecture.md), and [runbook](runbook.md) for acceptance checks, API/data contracts, startup/shutdown, logs, backups, and troubleshooting. The [README](../README.md#build-and-test) includes exact build/test commands for all Java modules.
