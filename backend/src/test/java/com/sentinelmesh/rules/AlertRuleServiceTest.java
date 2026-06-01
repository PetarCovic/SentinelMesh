package com.sentinelmesh.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sentinelmesh.TestDatabaseCleaner;
import com.sentinelmesh.TestQueueConfig;
import com.sentinelmesh.alerts.AlertSeverity;
import com.sentinelmesh.devices.DeviceType;
import com.sentinelmesh.events.SecurityEventSeverity;
import com.sentinelmesh.events.SecurityEventType;
import com.sentinelmesh.exceptions.AlertRuleNotFoundException;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestQueueConfig.class)
class AlertRuleServiceTest {

    @Autowired
    private AlertRuleService alertRuleService;

    @Autowired
    private AlertRuleRepository alertRuleRepository;

    @Autowired
    private TestDatabaseCleaner testDatabaseCleaner;

    @BeforeEach
    void setUp() {
        testDatabaseCleaner.clean();
    }

    @Test
    void createRule_shouldCreateAlertRule() {
        CreateAlertRuleRequest request = createRuleRequest();

        AlertRuleResponse response = alertRuleService.createRule(request);

        assertNotNull(response.getId());
        assertEquals("High severity events", response.getName());
        assertTrue(response.isEnabled());
        assertEquals(SecurityEventSeverity.HIGH, response.getMinimumSeverity());
        assertEquals(AlertSeverity.HIGH, response.getAlertSeverity());
        assertEquals(1, alertRuleRepository.count());
    }

    @Test
    void getAllRules_shouldReturnSavedRules() {
        alertRuleService.createRule(createRuleRequest());
        alertRuleService.createRule(createPersonDetectedRuleRequest());

        var rules = alertRuleService.getAllRules();

        assertEquals(2, rules.size());
    }

    @Test
    void getRuleById_shouldReturnRuleWhenItExists() {
        AlertRuleResponse created = alertRuleService.createRule(createRuleRequest());

        AlertRuleResponse found = alertRuleService.getRuleById(created.getId());

        assertEquals(created.getId(), found.getId());
        assertEquals("High severity events", found.getName());
    }

    @Test
    void getRuleById_shouldThrowWhenMissing() {
        UUID missingId = UUID.randomUUID();

        assertThrows(AlertRuleNotFoundException.class, () ->
                alertRuleService.getRuleById(missingId)
        );
    }

    @Test
    void updateRule_shouldUpdateProvidedFields() {
        AlertRuleResponse created = alertRuleService.createRule(createRuleRequest());

        UpdateAlertRuleRequest update = new UpdateAlertRuleRequest();
        update.setName("Updated Rule");
        update.disable();
        update.setMinimumSeverity(SecurityEventSeverity.CRITICAL);
        update.setAlertSeverity(AlertSeverity.CRITICAL);
        update.setAlertTitle("Updated title");
        update.setAlertMessage("Updated message");

        AlertRuleResponse updated = alertRuleService.updateRule(created.getId(), update);

        assertEquals("Updated Rule", updated.getName());
        assertFalse(updated.isEnabled());
        assertEquals(SecurityEventSeverity.CRITICAL, updated.getMinimumSeverity());
        assertEquals(AlertSeverity.CRITICAL, updated.getAlertSeverity());
        assertEquals("Updated title", updated.getAlertTitle());
        assertEquals("Updated message", updated.getAlertMessage());
    }

    @Test
    void enableRule_shouldEnableRule() {
        AlertRuleResponse created = alertRuleService.createRule(createDisabledRuleRequest());

        AlertRuleResponse enabled = alertRuleService.enableRule(created.getId());

        assertTrue(enabled.isEnabled());

        AlertRule reloaded = alertRuleRepository.findById(created.getId()).orElseThrow();
        assertTrue(reloaded.isEnabled());
    }

    @Test
    void disableRule_shouldDisableRule() {
        AlertRuleResponse created = alertRuleService.createRule(createRuleRequest());

        AlertRuleResponse disabled = alertRuleService.disableRule(created.getId());

        assertFalse(disabled.isEnabled());

        AlertRule reloaded = alertRuleRepository.findById(created.getId()).orElseThrow();
        assertFalse(reloaded.isEnabled());
    }

    @Test
    void deleteRule_shouldDeleteRule() {
        AlertRuleResponse created = alertRuleService.createRule(createRuleRequest());

        alertRuleService.deleteRule(created.getId());

        assertFalse(alertRuleRepository.existsById(created.getId()));
    }

    @Test
    void deleteRule_shouldThrowWhenRuleDoesNotExist() {
        UUID missingId = UUID.randomUUID();

        assertThrows(AlertRuleNotFoundException.class, () ->
                alertRuleService.deleteRule(missingId)
        );
    }

    private CreateAlertRuleRequest createRuleRequest() {
        CreateAlertRuleRequest request = new CreateAlertRuleRequest();
        request.setName("High severity events");
        request.setDescription("Creates alerts for HIGH and CRITICAL events");
        request.enable();
        request.setEventType(null);
        request.setMinimumSeverity(SecurityEventSeverity.HIGH);
        request.setDeviceType(null);
        request.setAlertSeverity(AlertSeverity.HIGH);
        request.setAlertTitle("{severity} security event: {eventType}");
        request.setAlertMessage("Device {deviceName} reported {eventType} at {deviceLocation}.");
        return request;
    }

    private CreateAlertRuleRequest createPersonDetectedRuleRequest() {
        CreateAlertRuleRequest request = new CreateAlertRuleRequest();
        request.setName("Person detected");
        request.setDescription("Creates alerts for person detections");
        request.enable();
        request.setEventType(SecurityEventType.PERSON_DETECTED);
        request.setMinimumSeverity(SecurityEventSeverity.MEDIUM);
        request.setDeviceType(DeviceType.CAMERA);
        request.setAlertSeverity(AlertSeverity.CRITICAL);
        request.setAlertTitle("Person detected by {deviceName}");
        request.setAlertMessage("{deviceName} detected a person with severity {severity}.");
        return request;
    }

    private CreateAlertRuleRequest createDisabledRuleRequest() {
        CreateAlertRuleRequest request = createRuleRequest();
        request.disable();
        return request;
    }
}