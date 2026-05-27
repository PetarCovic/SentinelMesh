package com.sentinelmesh.events;

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

import com.sentinelmesh.TestDatabaseCleaner;
import com.sentinelmesh.devices.ApiKeyHashService;
import com.sentinelmesh.devices.Device;
import com.sentinelmesh.devices.DeviceRepository;
import com.sentinelmesh.devices.DeviceType;
import com.sentinelmesh.exceptions.DeviceNotFoundException;
import com.sentinelmesh.exceptions.InvalidDeviceApiKeyException;
import com.sentinelmesh.exceptions.SecurityEventNotFoundException;

@SpringBootTest
@ActiveProfiles("test")
class SecurityEventServiceTest {

    @Autowired
    private SecurityEventService securityEventService;

    @Autowired
    private SecurityEventRepository securityEventRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private ApiKeyHashService apiKeyHashService;

    private String rawApiKey;

    @Autowired
    private TestDatabaseCleaner testDatabaseCleaner;

    @BeforeEach
    void setUp() {
        testDatabaseCleaner.clean();
 
        rawApiKey = "test";
    }

    @Test
    void createEvent_shouldPersistSecurityEvent() {
        Device savedDevice = createSavedDeviceWithApiKey();

        CreateSecurityEventRequest request = new CreateSecurityEventRequest();
        request.setEventType(SecurityEventType.MOTION_DETECTED);
        request.setSeverity(SecurityEventSeverity.MEDIUM);
        request.setConfidence(0.87);
        request.setMetadataJson("{\"zone\":\"front_porch\",\"motionArea\":0.42}");

        SecurityEventResponse response = securityEventService.createEvent(
                savedDevice.getId(),
                rawApiKey,
                request
        );

        assertNotNull(response.getId());
        assertEquals(savedDevice.getId(), response.getDeviceId());
        assertEquals("Front Door Camera", response.getDeviceName());
        assertEquals(SecurityEventType.MOTION_DETECTED, response.getEventType());
        assertEquals(SecurityEventSeverity.MEDIUM, response.getSeverity());
        assertEquals(0.87, response.getConfidence());
        assertNotNull(response.getOccurredAt());
        assertNotNull(response.getReceivedAt());
        assertEquals("{\"zone\":\"front_porch\",\"motionArea\":0.42}", response.getMetadataJson());

        assertEquals(1, securityEventRepository.count());
    }

    @Test
    void createEvent_shouldUseProvidedOccurredAt() {
        Device savedDevice = createSavedDeviceWithApiKey();

        Instant occurredAt = Instant.parse("2026-05-26T12:00:00Z");

        CreateSecurityEventRequest request = new CreateSecurityEventRequest();
        request.setEventType(SecurityEventType.PERSON_DETECTED);
        request.setSeverity(SecurityEventSeverity.HIGH);
        request.setConfidence(0.95);
        request.setOccurredAt(occurredAt);

        SecurityEventResponse response = securityEventService.createEvent(
                savedDevice.getId(),
                rawApiKey,
                request
        );

        assertEquals(occurredAt, response.getOccurredAt());
        assertNotNull(response.getReceivedAt());
    }

    @Test
    void createEvent_shouldDefaultOccurredAtWhenMissing() {
        Device savedDevice = createSavedDeviceWithApiKey();

        CreateSecurityEventRequest request = new CreateSecurityEventRequest();
        request.setEventType(SecurityEventType.DOOR_OPENED);
        request.setSeverity(SecurityEventSeverity.LOW);

        SecurityEventResponse response = securityEventService.createEvent(
                savedDevice.getId(),
                rawApiKey,
                request
        );

        assertNotNull(response.getOccurredAt());
        assertTrue(response.getOccurredAt().isBefore(Instant.now().plusSeconds(1)));
    }

    @Test
    void createEvent_shouldAllowNullConfidence() {
        Device savedDevice = createSavedDeviceWithApiKey();

        CreateSecurityEventRequest request = new CreateSecurityEventRequest();
        request.setEventType(SecurityEventType.DOOR_OPENED);
        request.setSeverity(SecurityEventSeverity.LOW);

        SecurityEventResponse response = securityEventService.createEvent(
                savedDevice.getId(),
                rawApiKey,
                request
        );

        assertNull(response.getConfidence());
        assertEquals(SecurityEventType.DOOR_OPENED, response.getEventType());
    }

    @Test
    void createEvent_shouldRejectInvalidApiKey() {
        Device savedDevice = createSavedDeviceWithApiKey();

        CreateSecurityEventRequest request = new CreateSecurityEventRequest();
        request.setEventType(SecurityEventType.MOTION_DETECTED);
        request.setSeverity(SecurityEventSeverity.MEDIUM);

        assertThrows(InvalidDeviceApiKeyException.class, () -> {
            securityEventService.createEvent(
                    savedDevice.getId(),
                    "wrong_key",
                    request
            );
        });

        assertEquals(0, securityEventRepository.count());
    }

    @Test
    void createEvent_shouldThrowWhenDeviceDoesNotExist() {
        UUID missingDeviceId = UUID.randomUUID();

        CreateSecurityEventRequest request = new CreateSecurityEventRequest();
        request.setEventType(SecurityEventType.MOTION_DETECTED);
        request.setSeverity(SecurityEventSeverity.MEDIUM);

        assertThrows(DeviceNotFoundException.class, () -> {
            securityEventService.createEvent(
                    missingDeviceId,
                    rawApiKey,
                    request
            );
        });

        assertEquals(0, securityEventRepository.count());
    }

    @Test
    void getAllEvents_shouldReturnSavedEvents() {
        Device savedDevice = createSavedDeviceWithApiKey();

        CreateSecurityEventRequest request1 = new CreateSecurityEventRequest();
        request1.setEventType(SecurityEventType.MOTION_DETECTED);
        request1.setSeverity(SecurityEventSeverity.MEDIUM);

        CreateSecurityEventRequest request2 = new CreateSecurityEventRequest();
        request2.setEventType(SecurityEventType.PERSON_DETECTED);
        request2.setSeverity(SecurityEventSeverity.HIGH);

        securityEventService.createEvent(savedDevice.getId(), rawApiKey, request1);
        securityEventService.createEvent(savedDevice.getId(), rawApiKey, request2);

        var events = securityEventService.getAllEvents();

        assertEquals(2, events.size());
    }

    @Test
    void getEventById_shouldReturnEventWhenItExists() {
        Device savedDevice = createSavedDeviceWithApiKey();

        CreateSecurityEventRequest request = new CreateSecurityEventRequest();
        request.setEventType(SecurityEventType.MOTION_DETECTED);
        request.setSeverity(SecurityEventSeverity.MEDIUM);

        SecurityEventResponse created = securityEventService.createEvent(
                savedDevice.getId(),
                rawApiKey,
                request
        );

        SecurityEventResponse found = securityEventService.getEventById(created.getId());

        assertEquals(created.getId(), found.getId());
        assertEquals(SecurityEventType.MOTION_DETECTED, found.getEventType());
    }

    @Test
    void getEventById_shouldThrowWhenEventDoesNotExist() {
        UUID missingEventId = UUID.randomUUID();

        assertThrows(SecurityEventNotFoundException.class, () -> {
            securityEventService.getEventById(missingEventId);
        });
    }

    @Test
    void getEventsByDevice_shouldReturnOnlyThatDevicesEvents() {
        Device device1 = createSavedDeviceWithApiKey("Front Door Camera", "Front Porch", rawApiKey);
        Device device2 = createSavedDeviceWithApiKey("Garage Camera", "Garage", "sm_live_other_key");

        CreateSecurityEventRequest request1 = new CreateSecurityEventRequest();
        request1.setEventType(SecurityEventType.MOTION_DETECTED);
        request1.setSeverity(SecurityEventSeverity.MEDIUM);

        CreateSecurityEventRequest request2 = new CreateSecurityEventRequest();
        request2.setEventType(SecurityEventType.DOOR_OPENED);
        request2.setSeverity(SecurityEventSeverity.LOW);

        securityEventService.createEvent(device1.getId(), rawApiKey, request1);
        securityEventService.createEvent(device2.getId(), "sm_live_other_key", request2);

        var device1Events = securityEventService.getEventsByDevice(device1.getId());

        assertEquals(1, device1Events.size());
        assertEquals(device1.getId(), device1Events.get(0).getDeviceId());
        assertEquals(SecurityEventType.MOTION_DETECTED, device1Events.get(0).getEventType());
    }

    private Device createSavedDeviceWithApiKey() {
        return createSavedDeviceWithApiKey(
                "Front Door Camera",
                "Front Porch",
                rawApiKey
        );
    }

    private Device createSavedDeviceWithApiKey(String name, String location, String apiKey) {
        Device device = new Device(
                name,
                DeviceType.CAMERA,
                location
        );

        device.setApiKeyHash(apiKeyHashService.hash(apiKey));

        return deviceRepository.save(device);
    }
}