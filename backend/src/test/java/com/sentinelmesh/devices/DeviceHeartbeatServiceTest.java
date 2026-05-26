package com.sentinelmesh.devices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.sentinelmesh.exceptions.DeviceNotFoundException;
import com.sentinelmesh.exceptions.InvalidDeviceApiKeyException;

@SpringBootTest
@ActiveProfiles("test")
class DeviceHeartbeatServiceTest {

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private ApiKeyHashService apiKeyHashService;

    private String rawApiKey;

    @BeforeEach
    void setUp() {
        deviceRepository.deleteAll();
        rawApiKey = "sm_live_test_heartbeat_key";
    }

    @Test
    void recordHeartbeat_shouldSetDeviceOnline() {
        Device savedDevice = createSavedDeviceWithApiKey();

        HeartbeatRequest request = new HeartbeatRequest();
        request.setStatus(DeviceStatus.ONLINE);

        DeviceResponse response = deviceService.recordHeartbeat(
                savedDevice.getId(),
                rawApiKey,
                request
        );

        assertEquals(savedDevice.getId(), response.getId());
        assertEquals(DeviceStatus.ONLINE, response.getStatus());
    }

    @Test
    void recordHeartbeat_shouldUpdateLastSeenAt() {
        Device savedDevice = createSavedDeviceWithApiKey();

        assertNull(savedDevice.getLastSeenAt());

        HeartbeatRequest request = new HeartbeatRequest();
        request.setStatus(DeviceStatus.ONLINE);

        DeviceResponse response = deviceService.recordHeartbeat(
                savedDevice.getId(),
                rawApiKey,
                request
        );

        assertNotNull(response.getLastSeenAt());
        assertTrue(response.getLastSeenAt().isBefore(Instant.now().plusSeconds(1)));
    }

    @Test
    void recordHeartbeat_shouldPersistStatusAndLastSeenAt() {
        Device savedDevice = createSavedDeviceWithApiKey();

        HeartbeatRequest request = new HeartbeatRequest();
        request.setStatus(DeviceStatus.ONLINE);

        deviceService.recordHeartbeat(savedDevice.getId(), rawApiKey, request);

        Device reloadedDevice = deviceRepository.findById(savedDevice.getId())
                .orElseThrow();

        assertEquals(DeviceStatus.ONLINE, reloadedDevice.getStatus());
        assertNotNull(reloadedDevice.getLastSeenAt());
    }

    @Test
    void recordHeartbeat_shouldRejectInvalidApiKey() {
        Device savedDevice = createSavedDeviceWithApiKey();

        HeartbeatRequest request = new HeartbeatRequest();
        request.setStatus(DeviceStatus.ONLINE);

        assertThrows(InvalidDeviceApiKeyException.class, () -> {
            deviceService.recordHeartbeat(
                    savedDevice.getId(),
                    "wrong_key",
                    request
            );
        });
    }

    @Test
    void recordHeartbeat_shouldThrowWhenDeviceDoesNotExist() {
        UUID missingId = UUID.randomUUID();

        HeartbeatRequest request = new HeartbeatRequest();
        request.setStatus(DeviceStatus.ONLINE);

        assertThrows(DeviceNotFoundException.class, () -> {
            deviceService.recordHeartbeat(
                    missingId,
                    rawApiKey,
                    request
            );
        });
    }

    private Device createSavedDeviceWithApiKey() {
        Device device = new Device(
                "Garage Camera",
                DeviceType.CAMERA,
                "Garage"
        );

        device.setApiKeyHash(apiKeyHashService.hash(rawApiKey));

        return deviceRepository.save(device);
    }
}