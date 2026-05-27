package com.sentinelmesh.devices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.sentinelmesh.TestDatabaseCleaner;
import com.sentinelmesh.exceptions.DeviceNotFoundException;
import com.sentinelmesh.exceptions.InvalidDeviceApiKeyException;

@SpringBootTest
@ActiveProfiles("test")
class DeviceAuthenticationServiceTest {

    @Autowired
    private DeviceAuthenticationService deviceAuthenticationService;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private ApiKeyHashService apiKeyHashService;

    @Autowired
    private TestDatabaseCleaner testDatabaseCleaner;

    @BeforeEach
    void setUp() {
        testDatabaseCleaner.clean();
    }

    @Test
    void authenticate_shouldReturnDeviceWhenApiKeyIsCorrect() {
        String rawApiKey = "sm_live_valid_test_key";
        String apiKeyHash = apiKeyHashService.hash(rawApiKey);

        Device device = new Device(
                "Front Door Camera",
                DeviceType.CAMERA,
                "Front Porch"
        );
        device.setApiKeyHash(apiKeyHash);

        Device savedDevice = deviceRepository.save(device);

        Device authenticatedDevice = deviceAuthenticationService.authenticate(
                savedDevice.getId(),
                rawApiKey
        );

        assertEquals(savedDevice.getId(), authenticatedDevice.getId());
        assertEquals("Front Door Camera", authenticatedDevice.getName());
    }

    @Test
    void authenticate_shouldThrowWhenApiKeyIsWrong() {
        String correctApiKey = "sm_live_correct_test_key";
        String wrongApiKey = "sm_live_wrong_test_key";

        Device device = new Device(
                "Backyard Camera",
                DeviceType.CAMERA,
                "Backyard"
        );
        device.setApiKeyHash(apiKeyHashService.hash(correctApiKey));

        Device savedDevice = deviceRepository.save(device);

        assertThrows(InvalidDeviceApiKeyException.class, () -> {
            deviceAuthenticationService.authenticate(savedDevice.getId(), wrongApiKey);
        });
    }

    @Test
    void authenticate_shouldThrowWhenDeviceDoesNotExist() {
        UUID missingId = UUID.randomUUID();

        assertThrows(DeviceNotFoundException.class, () -> {
            deviceAuthenticationService.authenticate(missingId, "sm_live_any_key");
        });
    }

    @Test
    void authenticate_shouldThrowWhenDeviceHasNoApiKeyHash() {
        Device device = new Device(
                "Garage Sensor",
                DeviceType.DOOR_SENSOR,
                "Garage"
        );

        Device savedDevice = deviceRepository.save(device);

        assertThrows(InvalidDeviceApiKeyException.class, () -> {
            deviceAuthenticationService.authenticate(savedDevice.getId(), "sm_live_any_key");
        });
    }
}