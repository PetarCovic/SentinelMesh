# Database Environments

SentinelMesh uses separate databases for development, testing, and benchmarking.

## Databases

- `sentinelmesh_dev`: normal local development and dashboard data
- `sentinelmesh_benchmark`: benchmark/load-test data
- H2 in-memory database: automated tests

## Profiles

Run development backend:

```cmd
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev

Run benchmark backend:
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=benchmark

Run tests:
mvnw.cmd clean test





Redis Queues

Development queue:
sentinelmesh:event-processing:dev

Benchmark queue:
sentinelmesh:event-processing:benchmark

Test queue:
sentinelmesh:event-processing:test