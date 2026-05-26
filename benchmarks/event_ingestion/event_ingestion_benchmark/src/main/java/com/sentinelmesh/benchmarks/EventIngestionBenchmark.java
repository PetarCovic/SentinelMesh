package com.sentinelmesh.benchmarks;

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

public class EventIngestionBenchmark {

    private static final String DEFAULT_BASE_URL = "http://localhost:8080";

    private static final Pattern ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern API_KEY_PATTERN = Pattern.compile("\"apiKey\"\\s*:\\s*\"([^\"]+)\"");

    private final HttpClient httpClient;
    private final String baseUrl;

    public EventIngestionBenchmark(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newHttpClient();
    }

    public static void main(String[] args) throws Exception {
        BenchmarkConfig config = BenchmarkConfig.fromArgs(args);

        EventIngestionBenchmark benchmark = new EventIngestionBenchmark(config.baseUrl());

        BenchmarkResult result = benchmark.run(config.eventCount());

        result.print();

        Path outputPath = result.saveToFile();
        System.out.println();
        System.out.println("Saved results to: " + outputPath.toAbsolutePath());
    }

    public BenchmarkResult run(int eventCount) throws IOException, InterruptedException {
        BenchmarkDevice device = createBenchmarkDevice();

        System.out.println("Created benchmark device:");
        System.out.println("  id: " + device.deviceId());
        System.out.println("  apiKey: " + device.apiKey());
        System.out.println();

        List<Long> latenciesMillis = new ArrayList<>();
        int failures = 0;

        long benchmarkStartNanos = System.nanoTime();

        for (int i = 0; i < eventCount; i++) {
            String eventJson = buildEventJson(i);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/devices/" + device.deviceId() + "/events"))
                    .header("Content-Type", "application/json")
                    .header("X-Device-Api-Key", device.apiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(eventJson))
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
                totalDurationMillis,
                latenciesMillis
        );
    }

    private BenchmarkDevice createBenchmarkDevice() throws IOException, InterruptedException {
        String json = """
                {
                  "name": "Benchmark Camera",
                  "type": "CAMERA",
                  "location": "Benchmark Lab"
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

    private static String buildEventJson(int index) {
        String[] eventTypes = {
                "MOTION_DETECTED",
                "PERSON_DETECTED",
                "DOOR_OPENED",
                "SOUND_DETECTED"
        };

        String[] severities = {
                "LOW",
                "MEDIUM",
                "HIGH",
                "CRITICAL"
        };

        String eventType = eventTypes[index % eventTypes.length];
        String severity = severities[index % severities.length];

        double confidence = 0.50 + ((index % 50) / 100.0);

        String metadataJson = String.format(
                "{\\\"benchmark\\\":true,\\\"sequence\\\":%d,\\\"zone\\\":\\\"benchmark_zone\\\",\\\"motionArea\\\":%.2f}",
                index,
                (index % 100) / 100.0
        );

        return String.format("""
                {
                  "eventType": "%s",
                  "severity": "%s",
                  "confidence": %.2f,
                  "occurredAt": "%s",
                  "metadataJson": "%s"
                }
                """, eventType, severity, confidence, Instant.now(), metadataJson);
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

    private record BenchmarkConfig(String baseUrl, int eventCount) {

        static BenchmarkConfig fromArgs(String[] args) {
            String baseUrl = DEFAULT_BASE_URL;
            int eventCount = 1000;

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

                    default -> throw new IllegalArgumentException("Unknown argument: " + args[i]);
                }
            }

            if (eventCount <= 0) {
                throw new IllegalArgumentException("--events must be greater than 0");
            }

            return new BenchmarkConfig(baseUrl, eventCount);
        }
    }

    private record BenchmarkResult(
            int totalRequests,
            int successfulRequests,
            int failedRequests,
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
            System.out.println("=== Event Ingestion Benchmark ===");
            System.out.println("Total requests:       " + totalRequests);
            System.out.println("Successful requests:  " + successfulRequests);
            System.out.println("Failed requests:      " + failedRequests);
            System.out.println("Total duration:       " + totalDurationMillis + " ms");
            System.out.printf("Events per second:    %.2f%n", eventsPerSecond);
            System.out.println();
            System.out.println("Latency:");
            System.out.printf("  Average:            %.2f ms%n", averageLatencyMillis);
            System.out.println("  Minimum:            " + minimumLatencyMillis + " ms");
            System.out.println("  Maximum:            " + maximumLatencyMillis + " ms");
            System.out.println("  p50:                " + p50LatencyMillis + " ms");
            System.out.println("  p95:                " + p95LatencyMillis + " ms");
            System.out.println("  p99:                " + p99LatencyMillis + " ms");
        }

        Path saveToFile() throws IOException {
            Path resultsDirectory = Path.of("results");
            Files.createDirectories(resultsDirectory);

            String fileName = "event-ingestion-" + System.currentTimeMillis() + ".json";
            Path outputPath = resultsDirectory.resolve(fileName);

            String json = String.format("""
                    {
                      "benchmark": "event-ingestion",
                      "timestamp": "%s",
                      "totalRequests": %d,
                      "successfulRequests": %d,
                      "failedRequests": %d,
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
                    Instant.now(),
                    totalRequests,
                    successfulRequests,
                    failedRequests,
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