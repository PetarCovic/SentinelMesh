package com.sentinelmesh.benchmark_tests;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EventAlertBenchmark {

    private static final String DEFAULT_BASE_URL = "http://localhost:8080";

    private static final Pattern ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern API_KEY_PATTERN = Pattern.compile("\"apiKey\"\\s*:\\s*\"([^\"]+)\"");

    private final HttpClient httpClient;
    private final String baseUrl;

    public EventAlertBenchmark(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newHttpClient();
    }

    public static void main(String[] args) throws Exception {
        BenchmarkConfig config = BenchmarkConfig.fromArgs(args);

        EventAlertBenchmark benchmark = new EventAlertBenchmark(config.baseUrl());

        BenchmarkResult result = benchmark.run(config.eventCount(), config.mode());

        result.print();

        Path outputPath = result.saveToFile(config.mode());
        System.out.println();
        System.out.println("Saved results to: " + outputPath.toAbsolutePath());
    }

    public BenchmarkResult run(int eventCount, BenchmarkMode mode) throws IOException, InterruptedException {
        BenchmarkDevice device = createBenchmarkDevice();

        System.out.println("Created benchmark device:");
        System.out.println("  id: " + device.deviceId());
        System.out.println("  apiKey: " + device.apiKey());
        System.out.println();

        List<Long> latenciesMillis = new ArrayList<>();
        int failures = 0;
        int expectedAlerts = 0;

        long benchmarkStartNanos = System.nanoTime();

        for (int i = 0; i < eventCount; i++) {
            EventPayload payload = buildEventPayload(i, mode);

            if (payload.shouldCreateAlert()) {
                expectedAlerts++;
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/devices/" + device.deviceId() + "/events"))
                    .header("Content-Type", "application/json")
                    .header("X-Device-Api-Key", device.apiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(payload.json()))
                    .build();

            long requestStartNanos = System.nanoTime();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            long elapsedMillis = nanosToMillis(System.nanoTime() - requestStartNanos);

            if (response.statusCode() == 201) {
                latenciesMillis.add(elapsedMillis);
            } else {
                failures++;

                if (failures <= 5) {
                    System.out.println("Failure #" + failures);
                    System.out.println("  Status: " + response.statusCode());
                    System.out.println("  Body: " + response.body());
                }
            }

            if ((i + 1) % 100 == 0) {
                System.out.println("Sent " + (i + 1) + "/" + eventCount + " events...");
            }
        }

        long totalDurationMillis = nanosToMillis(System.nanoTime() - benchmarkStartNanos);

        return BenchmarkResult.from(
                eventCount,
                latenciesMillis.size(),
                failures,
                expectedAlerts,
                totalDurationMillis,
                latenciesMillis
        );
    }

    private BenchmarkDevice createBenchmarkDevice() throws IOException, InterruptedException {
        String json = """
                {
                  "name": "Alert Benchmark Camera",
                  "type": "CAMERA",
                  "location": "Alert Benchmark Lab"
                }
                """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/devices"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        long startNanos = System.nanoTime();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        long elapsedMillis = nanosToMillis(System.nanoTime() - startNanos);

        if (response.statusCode() != 201) {
            throw new IllegalStateException(
                    "Failed to create benchmark device. Status: "
                            + response.statusCode()
                            + ", body: "
                            + response.body()
            );
        }

        String body = response.body();

        String deviceId = extract(body, ID_PATTERN, "id");
        String apiKey = extract(body, API_KEY_PATTERN, "apiKey");

        System.out.println("Device creation latency: " + elapsedMillis + " ms");

        return new BenchmarkDevice(deviceId, apiKey);
    }

    private static EventPayload buildEventPayload(int index, BenchmarkMode mode) {
        String eventType = switch (index % 4) {
            case 0 -> "PERSON_DETECTED";
            case 1 -> "MOTION_DETECTED";
            case 2 -> "DOOR_OPENED";
            default -> "SOUND_DETECTED";
        };

        String severity;

        if (mode == BenchmarkMode.ALERTS_ONLY) {
            severity = index % 2 == 0 ? "HIGH" : "CRITICAL";
        } else {
            severity = switch (index % 4) {
                case 0 -> "LOW";
                case 1 -> "MEDIUM";
                case 2 -> "HIGH";
                default -> "CRITICAL";
            };
        }

        boolean shouldCreateAlert = severity.equals("HIGH") || severity.equals("CRITICAL");

        double confidence = 0.70 + ((index % 30) / 100.0);

        String metadataJson = String.format(
                "{\\\"benchmark\\\":true,\\\"alertBenchmark\\\":true,\\\"sequence\\\":%d,\\\"zone\\\":\\\"alert_benchmark_zone\\\"}",
                index
        );

        String json = String.format("""
                {
                  "eventType": "%s",
                  "severity": "%s",
                  "confidence": %.2f,
                  "occurredAt": "%s",
                  "metadataJson": "%s"
                }
                """, eventType, severity, confidence, Instant.now(), metadataJson);

        return new EventPayload(json, shouldCreateAlert);
    }

    private static String extract(String text, Pattern pattern, String fieldName) {
        Matcher matcher = pattern.matcher(text);

        if (!matcher.find()) {
            throw new IllegalStateException("Could not extract field '" + fieldName + "' from response: " + text);
        }

        return matcher.group(1);
    }

    private static long nanosToMillis(long nanos) {
        return nanos / 1_000_000;
    }

    private record BenchmarkDevice(String deviceId, String apiKey) {
    }

    private record EventPayload(String json, boolean shouldCreateAlert) {
    }

    private enum BenchmarkMode {
        ALERTS_ONLY,
        MIXED;

        static BenchmarkMode fromString(String value) {
            return switch (value.toLowerCase()) {
                case "alerts-only" -> ALERTS_ONLY;
                case "mixed" -> MIXED;
                default -> throw new IllegalArgumentException(
                        "Unknown mode: " + value + ". Use alerts-only or mixed."
                );
            };
        }

        String fileNameValue() {
            return switch (this) {
                case ALERTS_ONLY -> "alerts-only";
                case MIXED -> "mixed";
            };
        }
    }

    private record BenchmarkConfig(String baseUrl, int eventCount, BenchmarkMode mode) {

        static BenchmarkConfig fromArgs(String[] args) {
            String baseUrl = DEFAULT_BASE_URL;
            int eventCount = 1000;
            BenchmarkMode mode = BenchmarkMode.ALERTS_ONLY;

            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--base-url" -> {
                        if (i + 1 >= args.length) {
                            throw new IllegalArgumentException("--base-url requires a value");
                        }

                        baseUrl = args[++i];
                    }

                    case "--events" -> {
                        if (i + 1 >= args.length) {
                            throw new IllegalArgumentException("--events requires a value");
                        }

                        eventCount = Integer.parseInt(args[++i]);
                    }

                    case "--mode" -> {
                        if (i + 1 >= args.length) {
                            throw new IllegalArgumentException("--mode requires a value");
                        }

                        mode = BenchmarkMode.fromString(args[++i]);
                    }

                    default -> throw new IllegalArgumentException("Unknown argument: " + args[i]);
                }
            }

            if (eventCount <= 0) {
                throw new IllegalArgumentException("--events must be greater than 0");
            }

            return new BenchmarkConfig(baseUrl, eventCount, mode);
        }
    }

    private record BenchmarkResult(
            int totalRequests,
            int successfulRequests,
            int failedRequests,
            int expectedAlertsCreated,
            long totalDurationMillis,
            double eventsPerSecond,
            double averageLatencyMillis,
            long minimumLatencyMillis,
            long maximumLatencyMillis,
            long p50LatencyMillis,
            long p95LatencyMillis,
            long p99LatencyMillis
    ) {

        static BenchmarkResult from(
                int totalRequests,
                int successfulRequests,
                int failedRequests,
                int expectedAlertsCreated,
                long totalDurationMillis,
                List<Long> latenciesMillis
        ) {
            List<Long> sortedLatencies = latenciesMillis.stream()
                    .sorted(Comparator.naturalOrder())
                    .toList();

            double eventsPerSecond = totalDurationMillis == 0
                    ? 0.0
                    : successfulRequests / (totalDurationMillis / 1000.0);

            double averageLatency = sortedLatencies.isEmpty()
                    ? 0.0
                    : sortedLatencies.stream()
                            .mapToLong(Long::longValue)
                            .average()
                            .orElse(0.0);

            long min = sortedLatencies.isEmpty() ? 0 : sortedLatencies.get(0);
            long max = sortedLatencies.isEmpty() ? 0 : sortedLatencies.get(sortedLatencies.size() - 1);

            return new BenchmarkResult(
                    totalRequests,
                    successfulRequests,
                    failedRequests,
                    expectedAlertsCreated,
                    totalDurationMillis,
                    eventsPerSecond,
                    averageLatency,
                    min,
                    max,
                    percentile(sortedLatencies, 50),
                    percentile(sortedLatencies, 95),
                    percentile(sortedLatencies, 99)
            );
        }

        private static long percentile(List<Long> sortedValues, int percentile) {
            if (sortedValues.isEmpty()) {
                return 0;
            }

            int index = (int) Math.ceil((percentile / 100.0) * sortedValues.size()) - 1;
            index = Math.max(0, Math.min(index, sortedValues.size() - 1));

            return sortedValues.get(index);
        }

        void print() {
            System.out.println();
            System.out.println("=== Event + Alert Benchmark ===");
            System.out.println("Total requests:          " + totalRequests);
            System.out.println("Successful requests:     " + successfulRequests);
            System.out.println("Failed requests:         " + failedRequests);
            System.out.println("Expected alerts created: " + expectedAlertsCreated);
            System.out.println("Total duration:          " + totalDurationMillis + " ms");
            System.out.printf("Events per second:       %.2f%n", eventsPerSecond);
            System.out.println();
            System.out.println("Latency:");
            System.out.printf("  Average:               %.2f ms%n", averageLatencyMillis);
            System.out.println("  Minimum:               " + minimumLatencyMillis + " ms");
            System.out.println("  Maximum:               " + maximumLatencyMillis + " ms");
            System.out.println("  p50:                   " + p50LatencyMillis + " ms");
            System.out.println("  p95:                   " + p95LatencyMillis + " ms");
            System.out.println("  p99:                   " + p99LatencyMillis + " ms");
        }

        Path saveToFile(BenchmarkMode mode) throws IOException {
            Path resultsDirectory = Path.of("results", "alert_results");
            Files.createDirectories(resultsDirectory);

            String fileName = "event-alert-" + mode.fileNameValue() + "-" + System.currentTimeMillis() + ".json";
            Path outputPath = resultsDirectory.resolve(fileName);

            String json = String.format("""
                    {
                      "benchmark": "event-alert-generation",
                      "mode": "%s",
                      "timestamp": "%s",
                      "totalRequests": %d,
                      "successfulRequests": %d,
                      "failedRequests": %d,
                      "expectedAlertsCreated": %d,
                      "totalDurationMillis": %d,
                      "eventsPerSecond": %.2f,
                      "latencyMillis": {
                        "average": %.2f,
                        "minimum": %d,
                        "maximum": %d,
                        "p50": %d,
                        "p95": %d,
                        "p99": %d
                      }
                    }
                    """,
                    mode.fileNameValue(),
                    Instant.now(),
                    totalRequests,
                    successfulRequests,
                    failedRequests,
                    expectedAlertsCreated,
                    totalDurationMillis,
                    eventsPerSecond,
                    averageLatencyMillis,
                    minimumLatencyMillis,
                    maximumLatencyMillis,
                    p50LatencyMillis,
                    p95LatencyMillis,
                    p99LatencyMillis
            );

            Files.writeString(outputPath, json);

            return outputPath;
        }
    }
}