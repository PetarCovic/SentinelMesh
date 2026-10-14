# Database environments

SentinelMesh uses PostgreSQL for runtime metadata, Redis for event evaluation work, and H2 with queue substitutes in backend tests.

## Profile and address matrix

| Mode / profile | Database | PostgreSQL address used by backend | Redis queue |
| --- | --- | --- | --- |
| Default host profile | `sentinelmesh` | `localhost:5432` | `sentinelmesh:event-processing` |
| `dev` | `sentinelmesh_dev` | `localhost:5432` | `sentinelmesh:event-processing:dev` |
| `benchmark` | `sentinelmesh_benchmark` | `localhost:5432` | `sentinelmesh:event-processing:benchmark` |
| `docker` | `POSTGRES_DB`, default `sentinelmesh_dev` | `postgres:5432` | `sentinelmesh:event-processing` |
| `test` | H2 `sentinelmesh_test` in memory | No PostgreSQL connection | Worker disabled; queue substitutes in relevant tests |

Compose publishes PostgreSQL as **host port 5433**, not 5432. Redis is `localhost:6379` for host processes and `redis:6379` inside the Docker network.

The `dev` and `benchmark` profiles isolate database/queue names, but they share the same relative media roots unless you override those too. Use separate media directories when fully isolating environments.

## Setup and schema

The root Compose file starts PostgreSQL 18. On first initialization of an empty named volume, the image creates the configured role and database. Existing volumes retain their databases and role passwords when `.env` changes.

The backend uses Hibernate `ddl-auto: update` in runtime profiles and `create-drop` for tests. No versioned migration tool is currently configured. Take a backup before changes to entities or an existing schema.

Compose does not automatically create `sentinelmesh_benchmark`. Create that database explicitly if you want to use it:

```powershell
docker compose exec postgres sh -c 'createdb -U "$POSTGRES_USER" sentinelmesh_benchmark'
```

Run this only when the database does not already exist. It creates an empty database; the benchmark-profile backend creates its tables on startup.

## Host development using Compose infrastructure

From the repository root:

```powershell
docker compose stop backend frontend
docker compose up -d postgres redis
$credential = [pscredential]::new("postgres", (Read-Host "PostgreSQL password" -AsSecureString))
$env:POSTGRES_PASSWORD = $credential.GetNetworkCredential().Password
Remove-Variable credential
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5433/sentinelmesh_dev"
$env:SPRING_DATASOURCE_USERNAME = "postgres"
Push-Location backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Use your actual database/role if they differ from the defaults. Stop with `Ctrl+C`, then `Pop-Location`.

For a benchmark backend, set the URL to `jdbc:postgresql://localhost:5433/sentinelmesh_benchmark` and use:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=benchmark"
```

That command is run from `backend/` with the same password/username environment. Do not run development and benchmark backends simultaneously on port 8080.

If using a separate PostgreSQL installation on `localhost:5432`, the YAML defaults already use that port; remove any old `SPRING_DATASOURCE_URL` override before switching. Restart an existing JVM after changing its environment.

## Tests

Select the test profile and clear host datasource overrides. From `backend/`:

```powershell
$env:SPRING_PROFILES_ACTIVE = "test"
Remove-Item Env:SPRING_DATASOURCE_URL, Env:SPRING_DATASOURCE_USERNAME, Env:SPRING_DATASOURCE_PASSWORD -ErrorAction SilentlyContinue
.\mvnw.cmd clean verify
```

Some test classes select `test` themselves, while the basic application context test does not. Explicitly selecting it for the full suite matches CI and keeps the tests on H2 with queue substitutes. This does not establish PostgreSQL-specific behavior, real Redis recovery, or media-storage correctness.

## Credentials and persisted data

- Compose reads the root ignored `.env`; host Java processes do not load that file automatically.
- `POSTGRES_PASSWORD` is required by runtime datasource settings. The committed example leaves it blank and Compose rejects a missing/empty value.
- A process environment setting can override a Compose `.env` value. Confirm the intended source without printing the password.
- Changing a Compose variable does not change an already-initialized database role password.
- The PostgreSQL 18 volume is mounted at `/var/lib/postgresql`; backend media is a separate bind mount.
- Preserve both database and media backups. See [runbook.md](runbook.md#data-preservation-and-backup).

Do not commit passwords, device keys, database dumps, or private camera media.
