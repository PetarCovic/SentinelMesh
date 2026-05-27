package com.sentinelmesh.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;

import com.sentinelmesh.TestDatabaseCleaner;
import com.sentinelmesh.devices.Device;
import com.sentinelmesh.devices.DeviceRepository;
import com.sentinelmesh.devices.DeviceType;

@DataJpaTest
@ActiveProfiles("test")
@Import(TestDatabaseCleaner.class)
class SecurityEventRepositoryTest {

    @Autowired
    private SecurityEventRepository securityEventRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    private Device savedDevice;

    @Autowired
    private TestDatabaseCleaner testDatabaseCleaner;

    @BeforeEach
    void setUp() {
        testDatabaseCleaner.clean();

        Device device = new Device(
                "Front Door Camera",
                DeviceType.CAMERA,
                "Front Porch"
        );

        savedDevice = deviceRepository.save(device);
    }

    @Test
    void save_shouldPersistSecurityEvent() {
        SecurityEvent event = new SecurityEvent(
                savedDevice,
                SecurityEventType.MOTION_DETECTED,
                SecurityEventSeverity.MEDIUM,
                0.87,
                Instant.now(),
                "{\"zone\":\"front_porch\"}"
        );

        SecurityEvent savedEvent = securityEventRepository.save(event);

        assertNotNull(savedEvent.getId());
        assertEquals(savedDevice.getId(), savedEvent.getDevice().getId());
        assertEquals(SecurityEventType.MOTION_DETECTED, savedEvent.getEventType());
        assertEquals(SecurityEventSeverity.MEDIUM, savedEvent.getSeverity());
        assertEquals(0.87, savedEvent.getConfidence());
        assertNotNull(savedEvent.getOccurredAt());
        assertNotNull(savedEvent.getReceivedAt());
        assertEquals("{\"zone\":\"front_porch\"}", savedEvent.getMetadataJson());
    }

    @Test
    void findByDevice_shouldReturnMatchingEvents() {
        SecurityEvent event1 = new SecurityEvent(
                savedDevice,
                SecurityEventType.MOTION_DETECTED,
                SecurityEventSeverity.MEDIUM,
                0.80,
                Instant.now(),
                null
        );

        SecurityEvent event2 = new SecurityEvent(
                savedDevice,
                SecurityEventType.PERSON_DETECTED,
                SecurityEventSeverity.HIGH,
                0.95,
                Instant.now(),
                null
        );

        securityEventRepository.save(event1);
        securityEventRepository.save(event2);

        var events = securityEventRepository.findByDevice(savedDevice);

        assertEquals(2, events.size());
    }

    @Test
    void findByDeviceId_shouldReturnMatchingEvents() {
        SecurityEvent event = new SecurityEvent(
                savedDevice,
                SecurityEventType.DOOR_OPENED,
                SecurityEventSeverity.LOW,
                null,
                Instant.now(),
                null
        );

        securityEventRepository.save(event);

        var events = securityEventRepository.findByDeviceId(savedDevice.getId());

        assertEquals(1, events.size());
        assertEquals(SecurityEventType.DOOR_OPENED, events.get(0).getEventType());
    }

    @Test
    void findByEventType_shouldReturnMatchingEvents() {
        securityEventRepository.save(new SecurityEvent(
                savedDevice,
                SecurityEventType.MOTION_DETECTED,
                SecurityEventSeverity.MEDIUM,
                0.80,
                Instant.now(),
                null
        ));

        securityEventRepository.save(new SecurityEvent(
                savedDevice,
                SecurityEventType.PERSON_DETECTED,
                SecurityEventSeverity.HIGH,
                0.95,
                Instant.now(),
                null
        ));

        var motionEvents = securityEventRepository.findByEventType(SecurityEventType.MOTION_DETECTED);

        assertEquals(1, motionEvents.size());
        assertEquals(SecurityEventType.MOTION_DETECTED, motionEvents.get(0).getEventType());
    }

    @Test
    void findBySeverity_shouldReturnMatchingEvents() {
        securityEventRepository.save(new SecurityEvent(
                savedDevice,
                SecurityEventType.MOTION_DETECTED,
                SecurityEventSeverity.MEDIUM,
                0.80,
                Instant.now(),
                null
        ));

        securityEventRepository.save(new SecurityEvent(
                savedDevice,
                SecurityEventType.PERSON_DETECTED,
                SecurityEventSeverity.HIGH,
                0.95,
                Instant.now(),
                null
        ));

        var highEvents = securityEventRepository.findBySeverity(SecurityEventSeverity.HIGH);

        assertEquals(1, highEvents.size());
        assertEquals(SecurityEventSeverity.HIGH, highEvents.get(0).getSeverity());
    }

    @Test
    void findByOccurredAtBetween_shouldReturnEventsWithinRange() {
        Instant now = Instant.now();

        securityEventRepository.save(new SecurityEvent(
                savedDevice,
                SecurityEventType.MOTION_DETECTED,
                SecurityEventSeverity.MEDIUM,
                0.80,
                now.minusSeconds(60),
                null
        ));

        securityEventRepository.save(new SecurityEvent(
                savedDevice,
                SecurityEventType.PERSON_DETECTED,
                SecurityEventSeverity.HIGH,
                0.95,
                now.minusSeconds(3600),
                null
        ));

        var recentEvents = securityEventRepository.findByOccurredAtBetween(
                now.minusSeconds(120),
                now.plusSeconds(10)
        );

        assertEquals(1, recentEvents.size());
        assertEquals(SecurityEventType.MOTION_DETECTED, recentEvents.get(0).getEventType());
    }
}