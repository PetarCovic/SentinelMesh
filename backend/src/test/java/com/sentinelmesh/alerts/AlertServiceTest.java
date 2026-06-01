package com.sentinelmesh.alerts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sentinelmesh.TestDatabaseCleaner;
import com.sentinelmesh.TestQueueConfig;
import com.sentinelmesh.devices.Device;
import com.sentinelmesh.devices.DeviceRepository;
import com.sentinelmesh.devices.DeviceType;
import com.sentinelmesh.events.SecurityEvent;
import com.sentinelmesh.events.SecurityEventRepository;
import com.sentinelmesh.events.SecurityEventSeverity;
import com.sentinelmesh.events.SecurityEventType;
import com.sentinelmesh.exceptions.AlertNotFoundException;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestQueueConfig.class)
class AlertServiceTest {

    @Autowired
    private AlertService alertService;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private SecurityEventRepository securityEventRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private TestDatabaseCleaner testDatabaseCleaner;

    @BeforeEach
    void setUp() {
        testDatabaseCleaner.clean();
    }

    @Test
    void createAlertIfNeeded_shouldCreateAlertForHighSeverityEvent() {
        SecurityEvent event = createSavedSecurityEvent(SecurityEventSeverity.HIGH);

        alertService.createAlertIfNeeded(event);

        assertEquals(1, alertRepository.count());

        Alert alert = alertRepository.findAll().get(0);

        assertEquals(event.getId(), alert.getSecurityEvent().getId());
        assertEquals(AlertSeverity.HIGH, alert.getSeverity());
        assertEquals(AlertStatus.OPEN, alert.getStatus());
        assertNotNull(alert.getTitle());
        assertNotNull(alert.getMessage());
        assertNotNull(alert.getCreatedAt());
    }

    @Test
    void createAlertIfNeeded_shouldCreateAlertForCriticalSeverityEvent() {
        SecurityEvent event = createSavedSecurityEvent(SecurityEventSeverity.CRITICAL);

        alertService.createAlertIfNeeded(event);

        assertEquals(1, alertRepository.count());

        Alert alert = alertRepository.findAll().get(0);

        assertEquals(AlertSeverity.CRITICAL, alert.getSeverity());
        assertEquals(AlertStatus.OPEN, alert.getStatus());
    }

    @Test
    void createAlertIfNeeded_shouldNotCreateAlertForLowSeverityEvent() {
        SecurityEvent event = createSavedSecurityEvent(SecurityEventSeverity.LOW);

        alertService.createAlertIfNeeded(event);

        assertEquals(0, alertRepository.count());
    }

    @Test
    void createAlertIfNeeded_shouldNotCreateAlertForMediumSeverityEvent() {
        SecurityEvent event = createSavedSecurityEvent(SecurityEventSeverity.MEDIUM);

        alertService.createAlertIfNeeded(event);

        assertEquals(0, alertRepository.count());
    }

    @Test
    void createAlertIfNeeded_shouldNotCreateDuplicateAlertForSameEvent() {
        SecurityEvent event = createSavedSecurityEvent(SecurityEventSeverity.HIGH);

        alertService.createAlertIfNeeded(event);
        alertService.createAlertIfNeeded(event);

        assertEquals(1, alertRepository.count());
    }

    @Test
    void getAllAlerts_shouldReturnSavedAlerts() {
        SecurityEvent event1 = createSavedSecurityEvent(SecurityEventSeverity.HIGH);
        SecurityEvent event2 = createSavedSecurityEvent(SecurityEventSeverity.CRITICAL);

        alertService.createAlertIfNeeded(event1);
        alertService.createAlertIfNeeded(event2);

        var alerts = alertService.getAllAlerts();

        assertEquals(2, alerts.size());
    }

    @Test
    void getOpenAlerts_shouldReturnOnlyOpenAlerts() {
        SecurityEvent event1 = createSavedSecurityEvent(SecurityEventSeverity.HIGH);
        SecurityEvent event2 = createSavedSecurityEvent(SecurityEventSeverity.CRITICAL);

        alertService.createAlertIfNeeded(event1);
        alertService.createAlertIfNeeded(event2);

        Alert alertToAcknowledge = alertRepository.findAll().get(0);
        alertService.acknowledgeAlert(alertToAcknowledge.getId());

        var openAlerts = alertService.getOpenAlerts();

        assertEquals(1, openAlerts.size());
        assertEquals(AlertStatus.OPEN, openAlerts.get(0).getStatus());
    }

    @Test
    void getOpenAlertsBySeverity_shouldReturnOnlyOpenAlertsWithMatchingSeverity() {
        SecurityEvent highEvent1 = createSavedSecurityEvent(SecurityEventSeverity.HIGH);
        SecurityEvent highEvent2 = createSavedSecurityEvent(SecurityEventSeverity.HIGH);
        SecurityEvent criticalEvent = createSavedSecurityEvent(SecurityEventSeverity.CRITICAL);

        alertService.createAlertIfNeeded(highEvent1);
        alertService.createAlertIfNeeded(highEvent2);
        alertService.createAlertIfNeeded(criticalEvent);

        Alert highAlertToAcknowledge = alertRepository.findAll()
                .stream()
                .filter(alert -> alert.getSeverity() == AlertSeverity.HIGH)
                .findFirst()
                .orElseThrow();

        alertService.acknowledgeAlert(highAlertToAcknowledge.getId());

        var openHighAlerts = alertService.getOpenAlertsBySeverity(AlertSeverity.HIGH);

        assertEquals(1, openHighAlerts.size());
        assertEquals(AlertSeverity.HIGH, openHighAlerts.get(0).getSeverity());
        assertEquals(AlertStatus.OPEN, openHighAlerts.get(0).getStatus());
    }

    @Test
    void getAlertById_shouldReturnAlertWhenItExists() {
        SecurityEvent event = createSavedSecurityEvent(SecurityEventSeverity.HIGH);

        alertService.createAlertIfNeeded(event);

        Alert savedAlert = alertRepository.findAll().get(0);

        AlertResponse response = alertService.getAlertById(savedAlert.getId());

        assertEquals(savedAlert.getId(), response.getId());
        assertEquals(AlertSeverity.HIGH, response.getSeverity());
        assertEquals(AlertStatus.OPEN, response.getStatus());
    }

    @Test
    void getAlertById_shouldThrowWhenAlertDoesNotExist() {
        UUID missingId = UUID.randomUUID();

        assertThrows(AlertNotFoundException.class, () -> {
            alertService.getAlertById(missingId);
        });
    }

    @Test
    void acknowledgeAlert_shouldUpdateStatusAndAcknowledgedAt() {
        SecurityEvent event = createSavedSecurityEvent(SecurityEventSeverity.HIGH);

        alertService.createAlertIfNeeded(event);

        Alert savedAlert = alertRepository.findAll().get(0);

        AlertResponse response = alertService.acknowledgeAlert(savedAlert.getId());

        assertEquals(AlertStatus.ACKNOWLEDGED, response.getStatus());
        assertNotNull(response.getAcknowledgedAt());
        assertNull(response.getResolvedAt());

        Alert reloadedAlert = alertRepository.findById(savedAlert.getId()).orElseThrow();

        assertEquals(AlertStatus.ACKNOWLEDGED, reloadedAlert.getStatus());
        assertNotNull(reloadedAlert.getAcknowledgedAt());
    }

    @Test
    void acknowledgeAlert_shouldThrowWhenAlertDoesNotExist() {
        UUID missingId = UUID.randomUUID();

        assertThrows(AlertNotFoundException.class, () -> {
            alertService.acknowledgeAlert(missingId);
        });
    }

    @Test
    void resolveAlert_shouldUpdateStatusAndResolvedAt() {
        SecurityEvent event = createSavedSecurityEvent(SecurityEventSeverity.HIGH);

        alertService.createAlertIfNeeded(event);

        Alert savedAlert = alertRepository.findAll().get(0);

        AlertResponse response = alertService.resolveAlert(savedAlert.getId());

        assertEquals(AlertStatus.RESOLVED, response.getStatus());
        assertNotNull(response.getResolvedAt());

        Alert reloadedAlert = alertRepository.findById(savedAlert.getId()).orElseThrow();

        assertEquals(AlertStatus.RESOLVED, reloadedAlert.getStatus());
        assertNotNull(reloadedAlert.getResolvedAt());
    }

    @Test
    void resolveAlert_shouldThrowWhenAlertDoesNotExist() {
        UUID missingId = UUID.randomUUID();

        assertThrows(AlertNotFoundException.class, () -> {
            alertService.resolveAlert(missingId);
        });
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