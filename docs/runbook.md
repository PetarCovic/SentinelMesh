# Operations runbook

Use this runbook for the current local Docker or host-development stack. Commands use Windows PowerShell from the repository root unless otherwise stated. Configuration and installation are in the [README](../README.md).

## Startup

### Docker stack

Ensure the ignored root `.env` exists and contains a non-empty `POSTGRES_PASSWORD`. Keep default ports for the supplied dashboard URLs.

```powershell
docker compose config --quiet
docker compose up -d --build
docker compose ps
docker compose logs --tail=100 backend
```

PostgreSQL and Redis have Compose health checks. The backend waits for those services at container startup; the frontend's dependency only controls startup order.

Check the running API and the database-backed device route:

```powershell
$base = "http://localhost:8080"
Invoke-RestMethod "$base/api/health"
Invoke-RestMethod "$base/api/devices"
```

Expect HTTP `200` from both. `/api/health` is a static process-level response, so it alone does not verify PostgreSQL or Redis. Inspect `/actuator/health` for configured dependency health and use the functional smoke test to verify event queue processing.

Open <http://localhost:5173>. Startup has finished for operational purposes when the dashboard can read devices and the functional event test can produce an alert.

### Host backend/frontend with Docker infrastructure

Follow the exact commands in [running-locally.md](running-locally.md#hybrid-development-mode). Stop containerized backend/frontend before starting host processes on the same ports. When using Compose PostgreSQL from the host, override the datasource URL to port `5433`.

In Eclipse, select the correct Spring profile and put the database password in the launch configuration's environment. Updating a Windows user environment variable does not update an already-running Eclipse process or JVM; restart the launcher/process after credential changes.

### Camera edge

Register a camera and match its UUID in [ConfigLoader.java](../cv_edge_node/src/main/java/com/sentinelmesh/edge/config/ConfigLoader.java). Set `SENTINEL_DEVICE_API_KEY` in the launch environment, and review the camera/video source and backend address.

From `cv_edge_node/`:

```powershell
mvn compile exec:java "-Dexec.mainClass=com.sentinelmesh.edge.CvEdgeNodeApplication"
```

Watch the terminal for camera capture and successful heartbeats. Query `GET /api/cameras/{deviceId}` to check `deviceStatus`, `liveStatus`, and `latestLiveFrameTime`. A successful heartbeat does not by itself establish that live frames are arriving.

## Shutdown and restart

1. Stop the camera edge with **Ctrl+C** in its terminal. Let its shutdown finish so the active recording can be finalized.
2. Stop host frontend/backend processes with **Ctrl+C** in their terminals.
3. Stop the Docker stack:

```powershell
docker compose down
```

Normal `down` preserves named PostgreSQL/Redis volumes and the backend media bind mount. Do not add `--volumes` for routine shutdown. `scripts/reset-docker.bat` is a separate destructive reset and asks for confirmation.

For a containerized backend restart:

```powershell
docker compose restart backend
```

To rebuild after Java source changes:

```powershell
docker compose up -d --build --no-deps backend
```

Backend restart clears in-memory live frames and disconnects dashboard WebSockets. The browser reconnects; the edge must publish fresh frames. A Redis volume does not by itself guarantee durable recovery of already-dequeued events.

## Log and data locations

| Component | Current log location / command | Related local data |
| --- | --- | --- |
| Container backend | `docker compose logs -f backend` | Host `backend/storage/`, mounted at `/app/storage` |
| PostgreSQL | `docker compose logs --tail=100 postgres` | Named `sentinelmesh_postgres_data` volume |
| Redis | `docker compose logs --tail=100 redis` | Named `sentinelmesh_redis_data` volume |
| Container frontend | `docker compose logs -f frontend` | Built bundle inside the container |
| Host backend/edge | Terminal output or Eclipse Console | Backend `storage/` relative to its working directory; edge `recording-spool/` |
| Browser frontend | Browser developer-tools Console and Network panels | No application database in the browser |
| Backend tests | `backend/target/surefire-reports/` | H2 in-memory database while tests run |
| CI | GitHub Actions job logs and uploaded backend test reports | Report artifacts retained according to the workflow |

The current backend configuration does not select a dedicated application log file. Do not assume that a `logs/` directory exists. When sharing logs, remove credentials and private media/device details.

## Functional smoke test

Use a local development database. This procedure creates one camera, one rule, one event, and an alert; it does not delete their linked records afterward.

### Register and authenticate

```powershell
$base = "http://localhost:8080"
$deviceBody = @{
    name = "Documentation Smoke Camera"
    type = "CAMERA"
    location = "Lab"
} | ConvertTo-Json -Compress

$camera = Invoke-RestMethod -Uri "$base/api/devices" -Method Post -ContentType "application/json" -Body $deviceBody
$cameraId = $camera.id
$headers = @{ "X-Device-Api-Key" = $camera.apiKey }

$camera | Select-Object id, name, status
$heartbeat = Invoke-RestMethod -Uri "$base/api/devices/$cameraId/heartbeat" -Method Post -ContentType "application/json" -Headers $headers -Body '{"status":"ONLINE"}'
$heartbeat | Select-Object id, status, lastSeenAt
```

Registration should return an initially `OFFLINE` device; the heartbeat should return `ONLINE` and a recent timestamp. Do not print the raw registration response because it contains the key.

### Configure a rule and ingest an event

```powershell
$ruleBody = @{
    name = "Documentation Smoke Motion Rule"
    description = "Manual verification"
    eventType = "MOTION_DETECTED"
    minimumSeverity = "HIGH"
    deviceType = "CAMERA"
    alertSeverity = "HIGH"
    alertTitle = "{deviceName}: {eventType}"
    alertMessage = "{deviceName} at {deviceLocation} reported {severity} motion."
} | ConvertTo-Json -Compress

$rule = Invoke-RestMethod -Uri "$base/api/rules" -Method Post -ContentType "application/json" -Body $ruleBody

$eventBody = @{
    eventType = "MOTION_DETECTED"
    severity = "HIGH"
    confidence = 0.9
    occurredAt = [DateTime]::UtcNow.ToString("o")
    metadataJson = '{"source":"manual-smoke-test"}'
} | ConvertTo-Json -Compress

$event = Invoke-RestMethod -Uri "$base/api/devices/$cameraId/events" -Method Post -ContentType "application/json" -Headers $headers -Body $eventBody
$event | Select-Object id, deviceId, eventType, severity, receivedAt
```

Look for the resulting alert, allowing up to 30 seconds for this diagnostic check:

```powershell
$matchingAlerts = @()
for ($attempt = 0; $attempt -lt 30; $attempt++) {
    $alerts = Invoke-RestMethod "$base/api/alerts"
    $matchingAlerts = @($alerts | Where-Object { $_.securityEventId -eq $event.id })
    if ($matchingAlerts.Count -gt 0) { break }
    Start-Sleep -Seconds 1
}
if ($matchingAlerts.Count -ne 1) {
    throw "Expected one alert for the event; inspect the backend worker and Redis queue."
}

$alertId = $matchingAlerts[0].id
Invoke-RestMethod -Uri "$base/api/alerts/$alertId/acknowledge" -Method Patch |
    Select-Object id, status, acknowledgedAt
Invoke-RestMethod -Uri "$base/api/alerts/$alertId/resolve" -Method Patch |
    Select-Object id, status, resolvedAt
```

Expect `ACKNOWLEDGED` followed by `RESOLVED`. If other enabled rules exist, the first match may create the alert instead of the new smoke rule; do not rely on the title to identify it.

### Check offline detection and reconnect

Do not run an edge/simulator heartbeat loop for this temporary camera during this check. Starting from its single manual heartbeat:

```powershell
Start-Sleep -Seconds 45
Invoke-RestMethod "$base/api/devices/$cameraId" |
    Select-Object id, status, lastSeenAt

Invoke-RestMethod -Uri "$base/api/devices/$cameraId/heartbeat" -Method Post -ContentType "application/json" -Headers $headers -Body '{"status":"ONLINE"}' |
    Select-Object id, status, lastSeenAt
```

Expect `OFFLINE` after the wait and `ONLINE` after reconnecting. The monitor's nominal window is 30–40 seconds; overloaded scheduling can take longer.

When finished, disable the smoke rule to avoid matching future events:

```powershell
Invoke-RestMethod -Uri "$base/api/rules/$($rule.id)/disable" -Method Patch |
    Select-Object id, enabled
```

Save the camera key privately first if you intend to use it again, then remove secret-bearing session variables:

```powershell
Remove-Variable camera, headers -ErrorAction SilentlyContinue
```

### Optional live/media check

With a real edge node running under its matching UUID and key:

```powershell
$deviceId = "<your running camera UUID>"
Invoke-RestMethod "$base/api/cameras/$deviceId" |
    Select-Object deviceStatus, liveStatus, latestLiveFrameTime, recordingAvailable, recordingSegmentCount
Start-Process "$base/api/devices/$deviceId/live"
```

Expect `ONLINE`/`LIVE` while fresh frames arrive. Verify snapshot/clip links from recent event responses and historical playback using the returned `videoUrl`. Check that disabling live overlays removes boxes from live preview and that stored recording/event media remains clean.

## Database connection failures

Check backend and PostgreSQL logs first:

```powershell
docker compose ps
docker compose logs --tail=100 postgres backend
```

| Symptom | Likely cause | Action |
| --- | --- | --- |
| Connection refused from host backend | Wrong port or PostgreSQL is stopped | Compose publishes `5433`; set `SPRING_DATASOURCE_URL` accordingly and confirm service health. |
| Connection refused inside backend container | Host address used inside Docker | Use the Docker profile with `postgres:5432`; `localhost` inside the container refers to that container. |
| Password authentication failed | Wrong process environment, a stale container, or an existing role still has the old password | Match the live PostgreSQL role password and client configuration. A new value in `.env` does not rotate the password in an initialized volume. |
| Database does not exist | Wrong profile/database name or an initialized volume with different databases | Check `POSTGRES_DB` and the datasource URL; create the intended database or select the existing one. |
| Port already allocated | Another service is using host `5433`, `6379`, `8080`, or `5173` | Stop the conflicting instance or update the complete address configuration. |

Inspect the database without printing its password:

```powershell
docker compose exec postgres sh -c 'psql -X -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

Inside `psql`, use `\conninfo` to see the connection, `\dt` to list application tables, and `\q` to exit. This local container socket connection does not prove that network password authentication is correct.

For an intended password rotation, take a database backup, change the existing role with interactive `\password`, update the ignored local configuration, and recreate/restart affected clients. Do not use a volume reset as a password-change procedure.

## Rejected requests

Use the backend's response status and body, then compare the request with [architecture.md](architecture.md#api-conventions).

| Status / symptom | Check |
| --- | --- |
| `400` | JSON field names/types, supported enum spelling, key header presence on protected routes, confidence bounds, or required multipart fields and time order |
| `401` | Current key belongs to the exact UUID in the route; restart a JVM that inherited an old key |
| `403` | Event/segment ownership matches the device when an ownership exception is raised |
| `404` | Correct API path and existing UUID; recording retrieval uses the stable edge-generated `segmentId`, not the backend row `id` |
| `409` | An event clip already exists |
| `413` | Multipart body exceeded the servlet limit |
| `500` | Storage failure, unhandled media exception, unavailable infrastructure, or another backend error; inspect logs |
| Browser CORS/WebSocket rejection | Open the dashboard at the configured `http://localhost:5173` origin and check its REST base setting and hardcoded WebSocket URL |

For protected JSON routes use `Content-Type: application/json` and `X-Device-Api-Key`. For multipart requests let the client generate the boundary. For an oversized snapshot/clip/recording, a storage-specific failure may currently produce a different status from the servlet-level `413`.

## Events accepted but alerts missing

1. Retrieve the event by UUID to confirm it exists.
2. Verify an **enabled** rule matches its event type, minimum severity, and device type.
3. Inspect worker logs for `Event processing worker error` or `Queued security event not found`.
4. Check Redis and the queue corresponding to the running profile:

```powershell
docker compose exec redis redis-cli PING
docker compose exec redis redis-cli LLEN sentinelmesh:event-processing
```

Docker uses `sentinelmesh:event-processing`; `dev` and `benchmark` use their profile-specific suffixes. A growing queue suggests the worker is not draining it. An empty queue does not prove an alert was created: an event may not match a rule, or evaluation may have failed after dequeue. There is currently no operator replay endpoint or dead-letter queue.

## Offline devices or missing live video

- Read both `status` and `lastSeenAt` from the device API; a manual heartbeat demonstrates only that one request, not a running heartbeat loop.
- The edge defaults to heartbeats every 10 seconds. Repeated `401` errors mean it is still using a mismatched UUID/key.
- Set the key in the same terminal or Eclipse launch environment and restart the edge; it is read when configuration is initialized.
- If the device is `ONLINE` but the camera is `STALE`, check frame capture/live-upload errors. Live frames expire after 10 seconds.
- An offline camera is `UNAVAILABLE` even if it has historical recordings.
- After a backend restart, allow the edge to upload a new live frame.
- If a stream stalls during detection/event uploads, inspect processing/upload timings; heartbeat and live freshness are separate observations.

### Localhost or IPv6 routing

For Windows setups where IPv4 localhost reaches a different service or proxy, test the IPv6 address directly:

```powershell
curl.exe --noproxy "*" -sS -o NUL -w "health HTTP %{http_code}\n" "http://[::1]:8080/api/health"
curl.exe --noproxy "*" -sS -o NUL -w "devices HTTP %{http_code}\n" "http://[::1]:8080/api/devices"
```

Use `$base = "http://[::1]:8080"` for diagnostic API commands if that address works. The edge already defaults to that address. The frontend REST/media client can use `VITE_API_BASE_URL` (restart/rebuild Vite afterward); its WebSocket constant and allowed browser origin still need consistent configuration. Changing `$base` in PowerShell does not change the dashboard.

## Data preservation and backup

The database and media files must be preserved together. Named-volume removal deletes database/queue data but does not erase the host `backend/storage/` bind mount; restoring only the database can leave missing media, and restoring only files can leave orphan files.

For a consistent local backup, pause camera/simulator writers and the backend before copying database/media data. With PostgreSQL still running, write a custom-format dump outside the repository:

```powershell
$backupDir = Join-Path $env:LOCALAPPDATA "SentinelMesh/backups"
New-Item -ItemType Directory -Force -Path $backupDir | Out-Null
$backupPath = Join-Path $backupDir ("sentinelmesh-{0}.dump" -f (Get-Date -Format "yyyyMMdd-HHmmss"))

docker compose exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc -f /tmp/sentinelmesh-backup.dump'
if ($LASTEXITCODE -ne 0) { throw "Database backup failed." }
docker cp sentinelmesh-postgres:/tmp/sentinelmesh-backup.dump $backupPath
if ($LASTEXITCODE -ne 0 -or (Get-Item $backupPath).Length -eq 0) { throw "Local backup could not be confirmed." }
docker exec sentinelmesh-postgres rm -f /tmp/sentinelmesh-backup.dump
```

Copy `backend/storage/` and, if preserving pending camera uploads, `cv_edge_node/recording-spool/` into the same private backup set. Resume the backend/writers afterward. Dumps and media may contain private data; keep them outside Git.

Automatic backend retention/reconciliation is not currently scheduled in the published code. Do not remove recording files manually while assuming their database rows will repair themselves.
