Create:

docs/running-locally.md

Add:

# Running SentinelMesh Locally

SentinelMesh can be run in two main ways:

1. Full Docker Compose mode
2. Hybrid development mode

---

## 1. Full Docker Compose Mode

This starts the full system:

- PostgreSQL
- Redis
- Spring Boot backend
- React frontend production build

From the project root:

```cmd
scripts\start-docker.bat

Or directly:

docker compose up --build

Frontend:

http://localhost:5173

Backend health check:

http://localhost:8080/actuator/health

Stop services:

scripts\stop-docker.bat

Reset Docker database and Redis volumes:

scripts\reset-docker.bat

This removes Docker volumes and deletes Docker-managed PostgreSQL/Redis data.

2. Hybrid Development Mode

Use this when developing backend/frontend locally while still using Docker for infrastructure.

Start only PostgreSQL and Redis:

scripts\start-infra.bat

Then run backend locally from the backend folder:

mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev

Then run frontend locally from the frontend folder:

npm run dev

Frontend:

http://localhost:5173

Backend:

http://localhost:8080

Stop infrastructure:

scripts\stop-infra.bat
Running Tests

Run all checks:

scripts\test-all.bat

Run backend tests only:

scripts\backend-tests.bat

Run frontend tests and build only:

scripts\frontend-tests.bat
Docker Logs

All logs:

scripts\docker-logs.bat

Specific service logs:

scripts\docker-logs.bat backend
scripts\docker-logs.bat frontend
scripts\docker-logs.bat postgres
scripts\docker-logs.bat redis
Manual API Smoke Test

Create a test device:

curl -X POST http://localhost:8080/api/devices ^
  -H "Content-Type: application/json" ^
  -d "{\"name\":\"Docker Test Camera\",\"type\":\"CAMERA\",\"location\":\"Docker Lab\"}"

The response includes an API key. Save it if you want to test heartbeats or event ingestion manually.

Common Issues
Backend connects to localhost:5432 inside Docker

Inside Docker, the backend should connect to:

postgres:5432

not:

localhost:5432

Check application-docker.yml and docker-compose.yml.

Port 5432 already in use

If local PostgreSQL is using port 5432, map Docker PostgreSQL to host port 5433:

ports:
  - "5433:5432"

The backend container still connects to:

postgres:5432
Redis port conflict

If another Redis container is using port 6379:

docker ps
docker stop sentinelmesh-redis

Then rerun Docker Compose.

Useful Docker Commands
docker compose ps
docker compose down
docker compose down -v
docker compose build --no-cache backend
docker compose logs -f backend

