# Database Environments

SentinelMesh uses separate database and queue settings for development, benchmarking, and automated tests.

## Databases

- `sentinelmesh_dev` — normal local development and dashboard data.
- `sentinelmesh_benchmark` — benchmark and load-test data.
- H2 in-memory database — automated backend tests.

The Docker Compose defaults use `sentinelmesh_dev`. The backend profiles use the PostgreSQL password from `POSTGRES_PASSWORD`.

## Spring profiles

Run the development profile from `backend/`:

```powershell
$env:POSTGRES_PASSWORD = "your-local-postgres-password"
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

Run the benchmark profile:

```powershell
$env:POSTGRES_PASSWORD = "your-local-postgres-password"
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=benchmark
```

The `dev` and `benchmark` profiles currently target PostgreSQL on `localhost:5432`. The Compose database is published on host port `5433`; adjust the local datasource configuration if you use the Compose database from a host process.

Run automated tests:

```powershell
.\mvnw.cmd clean test
```

The test profile uses an H2 in-memory database and does not require a local PostgreSQL server.

## Redis queues

The event-processing queue names are profile-specific:

- Development: `sentinelmesh:event-processing:dev`
- Benchmark: `sentinelmesh:event-processing:benchmark`
- Docker: `sentinelmesh:event-processing`

Redis runs on `localhost:6379` for host processes and is reached as `redis:6379` by the backend container.

## Credentials

Keep local credentials in `.env` or process environment variables. `.env` is ignored by Git. Do not place PostgreSQL passwords, device API keys, or other secrets in source files, test fixtures, logs, or documentation.
