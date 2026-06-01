package com.sentinelmesh.simulator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimulatedDeviceTest {

    @Test
    void simulatedDevice_shouldStoreDeviceFields() {
        SimulatedDevice device = new SimulatedDevice(
                "device-123",
                "api-key-123",
                "Simulated Camera 1",
                "CAMERA",
                "Zone 1"
        );

        assertEquals("device-123", device.id());
        assertEquals("api-key-123", device.apiKey());
        assertEquals("Simulated Camera 1", device.name());
        assertEquals("CAMERA", device.type());
        assertEquals("Zone 1", device.location());
    }
}