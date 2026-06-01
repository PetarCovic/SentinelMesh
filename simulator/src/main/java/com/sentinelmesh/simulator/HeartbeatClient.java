package com.sentinelmesh.simulator;

import java.io.IOException;
import java.util.Map;

public class HeartbeatClient {

    private final String baseUrl;
    private final HttpJsonClient httpJsonClient;

    public HeartbeatClient(String baseUrl, HttpJsonClient httpJsonClient) {
        this.baseUrl = baseUrl;
        this.httpJsonClient = httpJsonClient;
    }

    public void sendHeartbeat(SimulatedDevice device) throws IOException, InterruptedException {
        String url = baseUrl + "/api/devices/" + device.id() + "/heartbeat";

        String json = """
                {
                  "status": "ONLINE"
                }
                """;

        httpJsonClient.postJson(
                url,
                json,
                Map.of("X-Device-Api-Key", device.apiKey())
        );
    }
}