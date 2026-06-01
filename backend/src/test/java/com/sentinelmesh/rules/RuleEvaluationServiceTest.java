package com.sentinelmesh.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sentinelmesh.TestDatabaseCleaner;
import com.sentinelmesh.TestQueueConfig;
import com.sentinelmesh.alerts.Alert;
import com.sentinelmesh.alerts.AlertRepository;
import com.sentinelmesh.alerts.AlertSeverity;
import com.sentinelmesh.devices.Device;
import com.sentinelmesh.devices.DeviceRepository;
import com.sentinelmesh.devices.DeviceType;
import com.sentinelmesh.events.SecurityEvent;
import com.sentinelmesh.events.SecurityEventRepository;
import com.sentinelmesh.events.SecurityEventSeverity;
import com.sentinelmesh.events.SecurityEventType;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestQueueConfig.class)
class RuleEvaluationServiceTest {

    @Autowired
    private RuleEvaluationService ruleEvaluationService;

    @Autowired
    private AlertRuleRepository alertRuleRepository;

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
    void matches_shouldReturnTrueWhenRuleHasNoSpecificConditionsAndIsEnabled() {
        AlertRule rule = createRule(
                true,
                null,
                null,
                null,
                AlertSeverity.HIGH
        );

        SecurityEvent event = createSavedEvent(
                SecurityEventType.MOTION_DETECTED,
                SecurityEventSeverity.LOW,
                DeviceType.CAMERA
        );

        assertTrue(ruleEvaluationService.matches(rule, event));
    }

    @Test
    void matches_shouldReturnFalseWhenRuleIsDisabled() {
        AlertRule rule = createRule(
                false,
                null,
                SecurityEventSeverity.LOW,
                null,
                AlertSeverity.HIGH
        );

        SecurityEvent event = createSavedEvent(
                SecurityEventType.MOTION_DETECTED,
                SecurityEventSeverity.CRITICAL,
                DeviceType.CAMERA
        );

        assertFalse(ruleEvaluationService.matches(rule, event));
    }

    @Test
    void matches_shouldReturnFalseWhenEventTypeDoesNotMatch() {
        AlertRule rule = createRule(
                true,
                SecurityEventType.PERSON_DETECTED,
                null,
                null,
                AlertSeverity.HIGH
        );

        SecurityEvent event = createSavedEvent(
                SecurityEventType.MOTION_DETECTED,
                SecurityEventSeverity.HIGH,
                DeviceType.CAMERA
        );

        assertFalse(ruleEvaluationService.matches(rule, event));
    }

    @Test
    void matches_shouldReturnTrueWhenSeverityMeetsMinimumSeverity() {
        AlertRule rule = createRule(
                true,
                null,
                SecurityEventSeverity.HIGH,
                null,
                AlertSeverity.HIGH
        );

        SecurityEvent highEvent = createSavedEvent(
                SecurityEventType.MOTION_DETECTED,
                SecurityEventSeverity.HIGH,
                DeviceType.CAMERA
        );

        SecurityEvent criticalEvent = createSavedEvent(
                SecurityEventType.MOTION_DETECTED,
                SecurityEventSeverity.CRITICAL,
                DeviceType.CAMERA
        );

        assertTrue(ruleEvaluationService.matches(rule, highEvent));
        assertTrue(ruleEvaluationService.matches(rule, criticalEvent));
    }

    @Test
    void matches_shouldReturnFalseWhenSeverityIsBelowMinimumSeverity() {
        AlertRule rule = createRule(
                true,
                null,
                SecurityEventSeverity.HIGH,
                null,
                AlertSeverity.HIGH
        );

        SecurityEvent event = createSavedEvent(
                SecurityEventType.MOTION_DETECTED,
                SecurityEventSeverity.MEDIUM,
                DeviceType.CAMERA
        );

        assertFalse(ruleEvaluationService.matches(rule, event));
    }

    @Test
    void matches_shouldReturnFalseWhenDeviceTypeDoesNotMatch() {
        AlertRule rule = createRule(
                true,
                null,
                SecurityEventSeverity.LOW,
                DeviceType.MOTION_SENSOR,
                AlertSeverity.HIGH
        );

        SecurityEvent event = createSavedEvent(
                SecurityEventType.MOTION_DETECTED,
                SecurityEventSeverity.HIGH,
                DeviceType.CAMERA
        );

        assertFalse(ruleEvaluationService.matches(rule, event));
    }

    @Test
    void evaluate_shouldCreateAlertWhenEnabledRuleMatchesEvent() {
        AlertRule rule = createRule(
                true,
                SecurityEventType.PERSON_DETECTED,
                SecurityEventSeverity.HIGH,
                DeviceType.CAMERA,
                AlertSeverity.CRITICAL
        );
        alertRuleRepository.save(rule);

        SecurityEvent event = createSavedEvent(
                SecurityEventType.PERSON_DETECTED,
                SecurityEventSeverity.HIGH,
                DeviceType.CAMERA
        );

        ruleEvaluationService.evaluate(event);

        assertEquals(1, alertRepository.count());

        Alert alert = alertRepository.findAll().get(0);

        assertEquals(event.getId(), alert.getSecurityEvent().getId());
        assertEquals(AlertSeverity.CRITICAL, alert.getSeverity());
        assertTrue(alert.getTitle().contains("PERSON_DETECTED"));
        assertTrue(alert.getMessage().contains("Rule Test Camera"));
    }

    @Test
    void evaluate_shouldNotCreateAlertWhenNoRuleMatches() {
        AlertRule rule = createRule(
                true,
                SecurityEventType.PERSON_DETECTED,
                SecurityEventSeverity.HIGH,
                DeviceType.CAMERA,
                AlertSeverity.HIGH
        );
        alertRuleRepository.save(rule);

        SecurityEvent event = createSavedEvent(
                SecurityEventType.MOTION_DETECTED,
                SecurityEventSeverity.LOW,
                DeviceType.CAMERA
        );

        ruleEvaluationService.evaluate(event);

        assertEquals(0, alertRepository.count());
    }

    @Test
    void evaluate_shouldNotCreateAlertWhenRuleIsDisabled() {
        AlertRule rule = createRule(
                false,
                null,
                SecurityEventSeverity.LOW,
                null,
                AlertSeverity.HIGH
        );
        alertRuleRepository.save(rule);

        SecurityEvent event = createSavedEvent(
                SecurityEventType.MOTION_DETECTED,
                SecurityEventSeverity.CRITICAL,
                DeviceType.CAMERA
        );

        ruleEvaluationService.evaluate(event);

        assertEquals(0, alertRepository.count());
    }

    @Test
    void evaluate_shouldNotCreateDuplicateAlertForSameEvent() {
        AlertRule rule = createRule(
                true,
                null,
                SecurityEventSeverity.HIGH,
                null,
                AlertSeverity.HIGH
        );
        alertRuleRepository.save(rule);

        SecurityEvent event = createSavedEvent(
                SecurityEventType.PERSON_DETECTED,
                SecurityEventSeverity.HIGH,
                DeviceType.CAMERA
        );

        ruleEvaluationService.evaluate(event);
        ruleEvaluationService.evaluate(event);

        assertEquals(1, alertRepository.count());
    }

    private AlertRule createRule(
            boolean enabled,
            SecurityEventType eventType,
            SecurityEventSeverity minimumSeverity,
            DeviceType deviceType,
            AlertSeverity alertSeverity
    ) {
        return new AlertRule(
                "Test Rule",
                "Test rule description",
                enabled,
                eventType,
                minimumSeverity,
                deviceType,
                alertSeverity,
                "{severity} security event: {eventType}",
                "Device {deviceName} reported {eventType} with severity {severity} at {deviceLocation}."
        );
    }

    private SecurityEvent createSavedEvent(
            SecurityEventType eventType,
            SecurityEventSeverity severity,
            DeviceType deviceType
    ) {
        Device device = new Device(
                "Rule Test Camera",
                deviceType,
                "Front Porch"
        );

        Device savedDevice = deviceRepository.save(device);

        SecurityEvent event = new SecurityEvent(
                savedDevice,
                eventType,
                severity,
                0.93,
                Instant.now(),
                "{\"source\":\"rule-test\"}"
        );

        return securityEventRepository.save(event);
    }
}