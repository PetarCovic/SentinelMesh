package com.sentinelmesh.rules;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sentinelmesh.TestDatabaseCleaner;
import com.sentinelmesh.TestQueueConfig;
import com.sentinelmesh.alerts.AlertSeverity;
import com.sentinelmesh.events.SecurityEventSeverity;
import com.sentinelmesh.realtime.DashboardEventBroadcaster;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestQueueConfig.class)
class AlertRuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AlertRuleRepository alertRuleRepository;

    @Autowired
    private TestDatabaseCleaner testDatabaseCleaner;

    @MockitoBean
    private DashboardEventBroadcaster dashboardEventBroadcaster;

    @BeforeEach
    void setUp() {
        testDatabaseCleaner.clean();
    }

    @Test
    void createRule_shouldReturnCreatedRule() throws Exception {
        String json = """
                {
                  "name": "High severity events",
                  "description": "Creates alerts for HIGH and CRITICAL events",
                  "enabled": true,
                  "minimumSeverity": "HIGH",
                  "alertSeverity": "HIGH",
                  "alertTitle": "{severity} security event: {eventType}",
                  "alertMessage": "Device {deviceName} reported {eventType}."
                }
                """;

        mockMvc.perform(post("/api/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("High severity events"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.minimumSeverity").value("HIGH"))
                .andExpect(jsonPath("$.alertSeverity").value("HIGH"));
    }

    @Test
    void createRule_shouldReturnBadRequestWhenRequiredFieldsMissing() throws Exception {
        String json = """
                {
                  "description": "Missing required fields"
                }
                """;

        mockMvc.perform(post("/api/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllRules_shouldReturnRules() throws Exception {
        alertRuleRepository.save(new AlertRule(
                "High severity events",
                "Creates alerts for HIGH and CRITICAL events",
                true,
                null,
                SecurityEventSeverity.HIGH,
                null,
                AlertSeverity.HIGH,
                "High alert",
                "High alert message"
        ));

        mockMvc.perform(get("/api/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("High severity events"));
    }

    @Test
    void getRuleById_shouldReturnRule() throws Exception {
        AlertRule rule = alertRuleRepository.save(new AlertRule(
                "High severity events",
                "Creates alerts for HIGH and CRITICAL events",
                true,
                null,
                SecurityEventSeverity.HIGH,
                null,
                AlertSeverity.HIGH,
                "High alert",
                "High alert message"
        ));

        mockMvc.perform(get("/api/rules/{id}", rule.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(rule.getId().toString()))
                .andExpect(jsonPath("$.name").value("High severity events"));
    }

    @Test
    void getRuleById_shouldReturnNotFoundWhenMissing() throws Exception {
        UUID missingId = UUID.randomUUID();

        mockMvc.perform(get("/api/rules/{id}", missingId))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateRule_shouldUpdateRule() throws Exception {
        AlertRule rule = alertRuleRepository.save(new AlertRule(
                "Original Rule",
                "Original description",
                true,
                null,
                SecurityEventSeverity.HIGH,
                null,
                AlertSeverity.HIGH,
                "Original alert",
                "Original alert message"
        ));

        String json = """
                {
                  "name": "Updated Rule",
                  "enabled": false,
                  "minimumSeverity": "CRITICAL",
                  "alertSeverity": "CRITICAL",
                  "alertTitle": "Updated title",
                  "alertMessage": "Updated message"
                }
                """;

        mockMvc.perform(patch("/api/rules/{id}", rule.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Rule"))
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.minimumSeverity").value("CRITICAL"))
                .andExpect(jsonPath("$.alertSeverity").value("CRITICAL"))
                .andExpect(jsonPath("$.alertTitle").value("Updated title"))
                .andExpect(jsonPath("$.alertMessage").value("Updated message"));
    }

    @Test
    void enableRule_shouldEnableRule() throws Exception {
        AlertRule rule = alertRuleRepository.save(new AlertRule(
                "Disabled Rule",
                "Disabled description",
                false,
                null,
                SecurityEventSeverity.HIGH,
                null,
                AlertSeverity.HIGH,
                "Alert title",
                "Alert message"
        ));

        mockMvc.perform(patch("/api/rules/{id}/enable", rule.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void disableRule_shouldDisableRule() throws Exception {
        AlertRule rule = alertRuleRepository.save(new AlertRule(
                "Enabled Rule",
                "Enabled description",
                true,
                null,
                SecurityEventSeverity.HIGH,
                null,
                AlertSeverity.HIGH,
                "Alert title",
                "Alert message"
        ));

        mockMvc.perform(patch("/api/rules/{id}/disable", rule.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void deleteRule_shouldDeleteRule() throws Exception {
        AlertRule rule = alertRuleRepository.save(new AlertRule(
                "Delete Rule",
                "Delete description",
                true,
                null,
                SecurityEventSeverity.HIGH,
                null,
                AlertSeverity.HIGH,
                "Alert title",
                "Alert message"
        ));

        mockMvc.perform(delete("/api/rules/{id}", rule.getId()))
                .andExpect(status().isNoContent());

        assertFalse(alertRuleRepository.existsById(rule.getId()));
    }

    @Test
    void deleteRule_shouldReturnNotFoundWhenMissing() throws Exception {
        UUID missingId = UUID.randomUUID();

        mockMvc.perform(delete("/api/rules/{id}", missingId))
                .andExpect(status().isNotFound());
    }
}