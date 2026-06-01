package com.sentinelmesh.simulator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimulatorConfigTest {

    @Test
    void fromArgs_shouldUseDefaultsWhenNoArgsProvided() {
        SimulatorConfig config = SimulatorConfig.fromArgs(new String[]{});

        assertEquals("http://localhost:8080", config.baseUrl());
        assertEquals(5, config.deviceCount());
        assertEquals(60, config.durationSeconds());
        assertEquals(10, config.heartbeatIntervalSeconds());
        assertEquals(5, config.eventIntervalSeconds());
    }

    @Test
    void fromArgs_shouldParseCustomArgs() {
        SimulatorConfig config = SimulatorConfig.fromArgs(new String[]{
                "--base-url", "http://localhost:9090",
                "--devices", "10",
                "--duration-seconds", "120",
                "--heartbeat-interval-seconds", "15",
                "--event-interval-seconds", "3"
        });

        assertEquals("http://localhost:9090", config.baseUrl());
        assertEquals(10, config.deviceCount());
        assertEquals(120, config.durationSeconds());
        assertEquals(15, config.heartbeatIntervalSeconds());
        assertEquals(3, config.eventIntervalSeconds());
    }

    @Test
    void fromArgs_shouldThrowForUnknownArgument() {
        assertThrows(IllegalArgumentException.class, () ->
                SimulatorConfig.fromArgs(new String[]{"--bad-arg"})
        );
    }

    @Test
    void fromArgs_shouldThrowWhenDeviceCountIsInvalid() {
        assertThrows(IllegalArgumentException.class, () ->
                SimulatorConfig.fromArgs(new String[]{"--devices", "0"})
        );
    }

    @Test
    void fromArgs_shouldThrowWhenValueIsMissing() {
        assertThrows(IllegalArgumentException.class, () ->
                SimulatorConfig.fromArgs(new String[]{"--devices"})
        );
    }
}