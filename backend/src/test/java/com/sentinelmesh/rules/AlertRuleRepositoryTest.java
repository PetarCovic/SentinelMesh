package com.sentinelmesh.rules;

import com.sentinelmesh.TestDatabaseCleaner;
import com.sentinelmesh.alerts.AlertSeverity;
import com.sentinelmesh.events.SecurityEventSeverity;
import com.sentinelmesh.events.SecurityEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import(TestDatabaseCleaner.class)
class AlertRuleRepositoryTest {

    @Autowired
    private AlertRuleRepository alertRuleRepository;

    @Autowired
    private TestDatabaseCleaner testDatabaseCleaner;

    @BeforeEach
    void setUp() {
        testDatabaseCleaner.clean();
    }

    @Test
    void save_shouldPersistAlertRule() {
        AlertRule rule = new AlertRule(
                "High severity events",
                "Creates alerts for HIGH and CRITICAL events",
                true,
                null,
                SecurityEventSeverity.HIGH,
                null,
                AlertSeverity.HIGH,
                "{severity} security event: {eventType}",
                "Device {deviceName} reported {eventType}."
        );

        AlertRule savedRule = alertRuleRepository.save(rule);

        assertNotNull(savedRule.getId());
        assertEquals("High severity events", savedRule.getName());
        assertTrue(savedRule.isEnabled());
        assertEquals(SecurityEventSeverity.HIGH, savedRule.getMinimumSeverity());
        assertEquals(AlertSeverity.HIGH, savedRule.getAlertSeverity());
        assertNotNull(savedRule.getCreatedAt());
        assertNotNull(savedRule.getUpdatedAt());
    }

    @Test
    void findByEnabledTrue_shouldReturnOnlyEnabledRules() {
        AlertRule enabledRule = new AlertRule(
                "Enabled Rule",
                "Enabled rule description",
                true,
                SecurityEventType.PERSON_DETECTED,
                SecurityEventSeverity.HIGH,
                null,
                AlertSeverity.HIGH,
                "Enabled alert",
                "Enabled alert message"
        );

        AlertRule disabledRule = new AlertRule(
                "Disabled Rule",
                "Disabled rule description",
                false,
                SecurityEventType.MOTION_DETECTED,
                SecurityEventSeverity.HIGH,
                null,
                AlertSeverity.HIGH,
                "Disabled alert",
                "Disabled alert message"
        );

        alertRuleRepository.save(enabledRule);
        alertRuleRepository.save(disabledRule);

        var enabledRules = alertRuleRepository.findByEnabledTrue();

        assertEquals(1, enabledRules.size());
        assertEquals("Enabled Rule", enabledRules.get(0).getName());
        assertTrue(enabledRules.get(0).isEnabled());
    }

    @Test
    void deleteById_shouldRemoveRule() {
        AlertRule rule = alertRuleRepository.save(new AlertRule(
                "Temporary Rule",
                "Will be deleted",
                true,
                null,
                SecurityEventSeverity.HIGH,
                null,
                AlertSeverity.HIGH,
                "Temporary alert",
                "Temporary alert message"
        ));

        alertRuleRepository.deleteById(rule.getId());

        assertFalse(alertRuleRepository.existsById(rule.getId()));
    }
}