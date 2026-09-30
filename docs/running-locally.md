# Running SentinelMesh Locally

SentinelMesh supports a full Docker Compose run and a hybrid development run.

## Prerequisites

- Docker Desktop with Docker Compose
- Java 21 for backend and edge-node development
- Node.js and npm for frontend development
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

The current `dev` and `benchmark` Spring profiles expect PostgreSQL on `localhost:5432`. The Compose PostgreSQL service is published on host port `5433`, so use a PostgreSQL instance on port `5432` for these profiles, or adjust the datasource port in your local configuration before starting the backend.

Start Redis if PostgreSQL is already available locally:

```powershell
docker compose up -d redis
```

Set the password in the current PowerShell session:

```powershell
$env:POSTGRES_PASSWORD = "your-local-postgres-password"
```

Start the backend from `backend/`:

```powershell
Set-Location backend
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

In another terminal, start the frontend:

```powershell
Set-Location frontend
npm install
npm run dev
```

The frontend runs at <http://localhost:5173> and the backend at <http://localhost:8080>.

## Tests

Run all backend and frontend checks from the repository root:

```powershell
scripts\test-all.bat
```

Run only backend tests:

```powershell
Set-Location backend
.\mvnw.cmd clean test
```

Run frontend tests and the production build:

```powershell
Set-Location frontend
npm run test:run
npm run build
```

## Manual API smoke test

Create a temporary camera device:

```powershell
$body = @{ name = "Local Test Camera"; type = "CAMERA"; location = "Lab" } |
    ConvertTo-Json -Compress
Invoke-RestMethod "http://localhost:8080/api/devices" -Method Post `
    -ContentType "application/json" -Body $body
```

The response contains a device API key. Save it only in a local secret store or environment variable if you need to test a heartbeat. Do not commit it.

## Common issues

### The backend cannot connect to PostgreSQL

Inside Docker, use `postgres:5432`. From a process running on the host, use the host port configured for your PostgreSQL instance. Compose publishes PostgreSQL on `localhost:5433`.

### Compose refuses to start

Make sure `.env` exists and contains a non-empty `POSTGRES_PASSWORD`:

```powershell
Test-Path .env
Select-String -Path .env -Pattern '^POSTGRES_PASSWORD=.+$'
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
