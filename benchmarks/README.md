# SentinelMesh Benchmarks

This folder contains benchmark projects and benchmark result files for SentinelMesh.

Benchmarks are used to measure performance characteristics such as:

- API request latency
- Event ingestion throughput
- Success/failure rate
- p50, p95, and p99 latency
- Device simulation behavior

Current benchmarks:

- `event-ingestion/` — measures authenticated security event ingestion.

# Event Ingestion Benchmarks

This benchmark measures authenticated security event ingestion through:

```text
POST /api/devices/{deviceId}/events

To run Benchmarks:
cd into backend folder
mvnw.cmd spring-boot:run

In another terminal
cd into folder of pom.xml


Change in pom.xml by uncommenting the benchmark you want to run and commenting the others

To run EventIngestion
mvn exec:java -Dexec.args="--events 100"

To run EventAlert
mvn exec:java -Dexec.mainClass="com.sentinelmesh.benchmark_tests.EventAlertBenchmark" -Dexec.args="--events 100 --mode alerts-only"

To run Mixed
mvn exec:java -Dexec.mainClass=com.sentinelmesh.benchmark_tests.EventAlertBenchmark -Dexec.args="--events 100000 --mode mixed"