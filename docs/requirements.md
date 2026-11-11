# Requirements and acceptance criteria

This document describes behavior implemented in the current repository. A criterion describes an expected result; its presence here does not mean that a fresh test run or a production deployment has passed it. Verification combines the referenced automated tests with the [runbook smoke test](runbook.md#functional-smoke-test).

## Scope

SentinelMesh registers devices, authenticates selected device-originated requests, records heartbeats and security events, evaluates alert rules asynchronously, and exposes device/event/alert information to a dashboard. Camera media adds live preview, event snapshots/clips, and continuous recording retrieval.

External email/SMS delivery, MQTT ingestion, user accounts/RBAC, automatic recording reconciliation, and production deployment are outside the currently implemented core acceptance scope.

## Device registration

Request: `POST /api/devices` with JSON `name`, `type`, and optional `location`.

| ID | Behavior | Acceptance criterion |
| --- | --- | --- |
| REG-01 | Register a valid device | A nonblank name of at most 100 characters and a supported type produce HTTP `201`, a UUID, and the supplied device details. The smoke test supplies a nonblank location. |
| REG-02 | Validate input | Missing/blank name, an overlong name/location, missing type, or an invalid enum value produces HTTP `400`; the device is not created. |
| REG-03 | Initial status | A newly registered device is `OFFLINE` with no heartbeat timestamp. |
| REG-04 | Generate a credential | The creation response includes one generated raw `apiKey`; the database stores its SHA-256 hash, not the raw key. |
| REG-05 | Avoid returning credentials on reads | `GET /api/devices` and `GET /api/devices/{id}` return normal device fields without `apiKey` or its hash. |
| REG-06 | Missing device | Reading a UUID that is not registered returns HTTP `404`. |

Verification: [DeviceControllerTest](../backend/src/test/java/com/sentinelmesh/devices/DeviceControllerTest.java), [DeviceServiceTest](../backend/src/test/java/com/sentinelmesh/devices/DeviceServiceTest.java), [DeviceApiKeyServiceTest](../backend/src/test/java/com/sentinelmesh/devices/DeviceApiKeyServiceTest.java), and [ApiKeyHashServiceTest](../backend/src/test/java/com/sentinelmesh/devices/ApiKeyHashServiceTest.java).

A nullable location is permitted by the DTO, but registration's dashboard broadcast currently passes it to `Map.of`, which rejects null. Use a nonblank location in operational registration requests until that implementation gap is corrected.

## Heartbeats and offline detection

Request: `POST /api/devices/{id}/heartbeat`, header `X-Device-Api-Key`, JSON `{"status":"ONLINE"}`.

| ID | Behavior | Acceptance criterion |
| --- | --- | --- |
| HB-01 | Authenticate a heartbeat | A registered device with its current matching key receives HTTP `200`. |
| HB-02 | Refresh liveness | The backend sets the device to `ONLINE` and sets `lastSeenAt` using the backend clock. The service treats any valid heartbeat status as evidence of liveness rather than copying that status into the device. |
| HB-03 | Reject an incorrect key | A wrong key for an existing device produces HTTP `401` and does not refresh its liveness. |
| HB-04 | Reject an incomplete request | A missing key header, missing status, or invalid status enum produces HTTP `400`. An unknown device UUID produces HTTP `404`. |
| HB-05 | Keep a recently seen device online | The monitor leaves an online device unchanged while its heartbeat is no more than 30 seconds old. |
| HB-06 | Detect a stale device | The monitor runs every 10 seconds. An online device with `lastSeenAt` strictly before `now - 30 seconds`, or with a null timestamp, becomes `OFFLINE`. In a healthy scheduler, expect roughly 30–40 seconds after the last heartbeat, with possible scheduling delay. |
| HB-07 | Recover after reconnecting | A successful later heartbeat returns the device to `ONLINE` and advances `lastSeenAt`. Already-offline devices stay offline until another heartbeat. |

Verification: [DeviceHeartbeatControllerTest](../backend/src/test/java/com/sentinelmesh/devices/DeviceHeartbeatControllerTest.java), [DeviceHeartbeatServiceTest](../backend/src/test/java/com/sentinelmesh/devices/DeviceHeartbeatServiceTest.java), and [DeviceStatusMonitorTest](../backend/src/test/java/com/sentinelmesh/devices/DeviceStatusMonitorTest.java). The scheduler timing and stopped-edge behavior also need the manual smoke test.

Offline device status and live-frame freshness are separate. For an online camera, no fresh JPEG yields `STALE`; an offline camera yields `UNAVAILABLE`. A live frame is stale after 10 seconds, independently of the 30-second heartbeat threshold. The status monitor currently does not broadcast its offline transitions, so dashboard polling is needed to observe them.

## Event ingestion

Request: `POST /api/devices/{deviceId}/events` with `X-Device-Api-Key` and JSON.

| ID | Behavior | Acceptance criterion |
| --- | --- | --- |
| EVT-01 | Accept a valid event | Required `eventType` and `severity`, plus a valid device/key, produce HTTP `201` with an event UUID and its device ID. |
| EVT-02 | Validate event fields | Missing type/severity, an invalid enum value, confidence outside `[0,1]`, or metadata longer than 5,000 characters produces HTTP `400`. Confidence may be omitted. |
| EVT-03 | Separate occurrence and receipt time | A supplied ISO-8601 `occurredAt` is preserved. If omitted, the backend supplies it. `receivedAt` records backend receipt. |
| EVT-04 | Protect the ingestion route | An incorrect key produces HTTP `401`; a missing key header produces HTTP `400`; an unknown device produces HTTP `404`. No successful event response is returned. |
| EVT-05 | Queue rule evaluation | The service saves the event and enqueues its UUID. The worker later retrieves the event and evaluates alert rules. HTTP `201` does not mean alert evaluation has completed. |
| EVT-06 | Retrieve events | The event can be read by UUID and device, and appears in `/api/events/recent` ordered by newest receipt time. |
| EVT-07 | Bound recent-event pages | Page defaults are `page=0`, `size=50`; the service clamps negative page to zero and size to `1..100`. |
| EVT-08 | Return media availability | A new event initially has no attached media. Later reads expose snapshot/clip IDs and relative URLs when their metadata exists. |

Verification: [SecurityEventControllerTest](../backend/src/test/java/com/sentinelmesh/events/SecurityEventControllerTest.java), [SecurityEventServiceTest](../backend/src/test/java/com/sentinelmesh/events/SecurityEventServiceTest.java), and [SecurityEventServiceAsyncTest](../backend/src/test/java/com/sentinelmesh/events/SecurityEventServiceAsyncTest.java).

`metadataJson` is currently a string with a length limit; the ingestion DTO does not validate its contents as structured JSON. Repeated event POSTs are not deduplicated by a client event ID.

## Alert rules and alert lifecycle

| ID | Behavior | Acceptance criterion |
| --- | --- | --- |
| ALT-01 | Configure a rule | A rule has a nonblank name/title/message and an alert severity. Optional conditions are event type, minimum event severity, and device type. |
| ALT-02 | Match enabled rules | Each configured condition must match. Null conditions act as wildcards. Disabled rules never match. |
| ALT-03 | Compare severities | Event severity order is `LOW < MEDIUM < HIGH < CRITICAL`; the minimum is inclusive. |
| ALT-04 | Create an alert asynchronously | After the worker evaluates an event against an enabled matching rule, an `OPEN` alert is stored for that event. Its severity comes from the rule. |
| ALT-05 | Avoid duplicate alerts for an event | Re-evaluating an event that already has an alert does not create another. A unique `security_event_id` also constrains the table. |
| ALT-06 | Handle multiple/no matches | Evaluation stops after its first matching rule. With no matching enabled rule, no alert is created. Rule order is not explicitly prioritized. |
| ALT-07 | Expand templates | Titles support `{eventType}`, `{severity}`, and `{deviceName}`. Messages also support `{deviceLocation}`. |
| ALT-08 | Acknowledge and resolve | The acknowledge endpoint returns `ACKNOWLEDGED` and records `acknowledgedAt`; resolve returns `RESOLVED` and records `resolvedAt`. The current methods do not enforce a strict transition sequence. |
| ALT-09 | Read open and recent alerts | Open pages contain only `OPEN` alerts. Recent pages are sorted newest first. A missing alert UUID returns HTTP `404`. |
| ALT-10 | Update the dashboard | Successful rule/alert operations broadcast dashboard messages. Connected clients refresh relevant REST data; messages are not an alert delivery acknowledgement. |

Verification: [RuleEvaluationServiceTest](../backend/src/test/java/com/sentinelmesh/rules/RuleEvaluationServiceTest.java), [AlertControllerTest](../backend/src/test/java/com/sentinelmesh/alerts/AlertControllerTest.java), [AlertServiceTest](../backend/src/test/java/com/sentinelmesh/alerts/AlertServiceTest.java), and the [runbook smoke test](runbook.md#functional-smoke-test) with real Redis.

The normal event worker uses `RuleEvaluationService`. The older severity-based `AlertService.createAlertIfNeeded` method is not called by that worker. A high-severity event alone therefore does not ensure an alert; create an enabled matching rule.

## Camera/media acceptance scope

| ID | Behavior | Acceptance criterion |
| --- | --- | --- |
| MED-01 | Camera overview | Camera APIs return status, live state, recording availability/count/range, and recent event media links. A missing camera is `404`; a non-camera device on camera detail is `400`. |
| MED-02 | Live preview | A nonempty JPEG no larger than 2,000,000 bytes from a registered camera is accepted at the live-frame route with HTTP `204`; preview uses an MJPEG response. |
| MED-03 | Recording upload retry | Reusing a stable recording `segmentId` for the same device returns the existing segment rather than creating another. A sequential retry is covered by the implementation; concurrent retry behavior still needs stress verification. |
| MED-04 | Historical media | Metadata routes return relative video/image URLs. Stored media remains available while a device is offline, provided both the metadata and file are present. |
| MED-05 | Live overlay separation | With live overlay enabled, the edge annotates a copied frame. Continuous recordings, event snapshots, and event clips retain the unannotated source. Overlay failure falls back to a clean live frame. |

The GitHub backend test tree currently has no dedicated camera/live/recording test classes. Treat these as manual acceptance checks rather than established automated coverage.

## Verification commands and limits

From the repository root in PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE = "test"
Remove-Item Env:SPRING_DATASOURCE_URL, Env:SPRING_DATASOURCE_USERNAME, Env:SPRING_DATASOURCE_PASSWORD -ErrorAction SilentlyContinue
Push-Location backend
.\mvnw.cmd clean verify
Pop-Location
Push-Location frontend
npm ci
npm run test:run
npm run build
Pop-Location
```

Test reports are written to `backend/target/surefire-reports/`. See the [README](../README.md#build-and-test) for edge/simulator checks.

With the test profile selected and runtime datasource overrides cleared, the tests use H2 and controlled queue substitutes. It does not establish Redis crash recovery, PostgreSQL-specific transaction behavior, real camera accuracy, load capacity, or end-to-end timing. Redis list dequeue has no acknowledgement/retry/dead-letter mechanism in the current worker, and database commit/enqueue are not one atomic operation. These remain implementation and integration-test gaps rather than guaranteed delivery properties.
