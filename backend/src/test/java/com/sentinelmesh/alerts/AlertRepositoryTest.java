package com.sentinelmesh.alerts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import com.sentinelmesh.events.SecurityEvent;
import com.sentinelmesh.events.SecurityEventRepository;
import com.sentinelmesh.events.SecurityEventSeverity;
import com.sentinelmesh.events.SecurityEventType;

@DataJpaTest
@ActiveProfiles("test")
@Import(TestDatabaseCleaner.class)
class AlertRepositoryTest {

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private SecurityEventRepository securityEventRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    private SecurityEvent savedHighEvent;
    private SecurityEvent savedCriticalEvent;

    @Autowired
    private TestDatabaseCleaner testDatabaseCleaner;

    @BeforeEach
    void setUp()
    {
        testDatabaseCleaner.clean();

        savedHighEvent = createSavedSecurityEvent(SecurityEventSeverity.HIGH);
        savedCriticalEvent = createSavedSecurityEvent(SecurityEventSeverity.CRITICAL);
    }

    @Test
    void save_shouldPersistAlert() {
        Alert alert = new Alert(
                savedHighEvent,
                AlertSeverity.HIGH,
                "HIGH security event: PERSON_DETECTED",
                "Device Front Door Camera reported PERSON_DETECTED with severity HIGH."
        );

        Alert savedAlert = alertRepository.save(alert);

        assertNotNull(savedAlert.getId());
        assertEquals(savedHighEvent.getId(), savedAlert.getSecurityEvent().getId());
        assertEquals(AlertSeverity.HIGH, savedAlert.getSeverity());
        assertEquals(AlertStatus.OPEN, savedAlert.getStatus());
        assertNotNull(savedAlert.getCreatedAt());
    }

    @Test
    void findByStatus_shouldReturnMatchingAlerts() {
        Alert openAlert = new Alert(
                savedHighEvent,
                AlertSeverity.HIGH,
                "High Alert",
                "High alert message"
        );

        Alert acknowledgedAlert = new Alert(
                savedCriticalEvent,
                AlertSeverity.CRITICAL,
                "Critical Alert",
                "Critical alert message"
        );
        acknowledgedAlert.acknowledge();

        alertRepository.save(openAlert);
        alertRepository.save(acknowledgedAlert);

        var openAlerts = alertRepository.findByStatus(AlertStatus.OPEN);

        assertEquals(1, openAlerts.size());
        assertEquals(AlertStatus.OPEN, openAlerts.get(0).getStatus());
    }

    @Test
    void findBySeverity_shouldReturnMatchingAlerts() {
        Alert highAlert = new Alert(
                savedHighEvent,
                AlertSeverity.HIGH,
                "High Alert",
                "High alert message"
        );

        Alert criticalAlert = new Alert(
                savedCriticalEvent,
                AlertSeverity.CRITICAL,
                "Critical Alert",
                "Critical alert message"
        );

        alertRepository.save(highAlert);
        alertRepository.save(criticalAlert);

        var highAlerts = alertRepository.findBySeverity(AlertSeverity.HIGH);

        assertEquals(1, highAlerts.size());
        assertEquals(AlertSeverity.HIGH, highAlerts.get(0).getSeverity());
    }

    @Test
    void findByStatusAndSeverity_shouldReturnMatchingOpenSeverityAlerts() {
        Alert openHighAlert = new Alert(
                savedHighEvent,
                AlertSeverity.HIGH,
                "Open High Alert",
                "Open high alert message"
        );

        Alert acknowledgedCriticalAlert = new Alert(
                savedCriticalEvent,
                AlertSeverity.CRITICAL,
                "Acknowledged Critical Alert",
                "Acknowledged critical alert message"
        );
        acknowledgedCriticalAlert.acknowledge();

        alertRepository.save(openHighAlert);
        alertRepository.save(acknowledgedCriticalAlert);

        var openHighAlerts = alertRepository.findByStatusAndSeverity(
                AlertStatus.OPEN,
                AlertSeverity.HIGH
        );

        assertEquals(1, openHighAlerts.size());
        assertEquals(AlertStatus.OPEN, openHighAlerts.get(0).getStatus());
        assertEquals(AlertSeverity.HIGH, openHighAlerts.get(0).getSeverity());
    }

    @Test
    void existsBySecurityEventId_shouldReturnTrueWhenAlertExistsForEvent() {
        Alert alert = new Alert(
                savedHighEvent,
                AlertSeverity.HIGH,
                "High Alert",
                "High alert message"
        );

        alertRepository.save(alert);

        assertTrue(alertRepository.existsBySecurityEventId(savedHighEvent.getId()));
    }

    @Test
    void existsBySecurityEventId_shouldReturnFalseWhenAlertDoesNotExistForEvent() {
        assertFalse(alertRepository.existsBySecurityEventId(savedHighEvent.getId()));
    }

    private SecurityEvent createSavedSecurityEvent(SecurityEventSeverity severity) {
        Device device = new Device(
                "Front Door Camera",
                DeviceType.CAMERA,
                "Front Porch"
        );

        Device savedDevice = deviceRepository.save(device);

        SecurityEvent event = new SecurityEvent(
                savedDevice,
                SecurityEventType.PERSON_DETECTED,
                severity,
                0.93,
                Instant.now(),
                "{\"zone\":\"front_porch\"}"
        );

        return securityEventRepository.save(event);
    }
}