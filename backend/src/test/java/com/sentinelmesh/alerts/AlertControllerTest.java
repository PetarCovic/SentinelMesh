package com.sentinelmesh.alerts;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.sentinelmesh.TestDatabaseCleaner;
import com.sentinelmesh.devices.Device;
import com.sentinelmesh.devices.DeviceRepository;
import com.sentinelmesh.devices.DeviceType;
import com.sentinelmesh.events.SecurityEvent;
import com.sentinelmesh.events.SecurityEventRepository;
import com.sentinelmesh.events.SecurityEventSeverity;
import com.sentinelmesh.events.SecurityEventType;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
    void getAllAlerts_shouldReturnAlerts() throws Exception {
        Alert alert = createSavedAlert(AlertSeverity.HIGH);

        mockMvc.perform(get("/api/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(alert.getId().toString()))
                .andExpect(jsonPath("$[0].severity").value("HIGH"))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].deviceName").value("Front Door Camera"));
    }

    @Test
    void getOpenAlerts_shouldReturnOnlyOpenAlerts() throws Exception {
        Alert openAlert = createSavedAlert(AlertSeverity.HIGH);
        Alert acknowledgedAlert = createSavedAlert(AlertSeverity.CRITICAL);
        acknowledgedAlert.acknowledge();
        alertRepository.save(acknowledgedAlert);

        mockMvc.perform(get("/api/alerts/open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(openAlert.getId().toString()))
                .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    @Test
    void getOpenAlertsBySeverity_shouldReturnOnlyOpenAlertsWithMatchingSeverity() throws Exception {
        Alert openHighAlert = createSavedAlert(AlertSeverity.HIGH);

        Alert acknowledgedHighAlert = createSavedAlert(AlertSeverity.HIGH);
        acknowledgedHighAlert.acknowledge();
        alertRepository.save(acknowledgedHighAlert);

        createSavedAlert(AlertSeverity.CRITICAL);

        mockMvc.perform(get("/api/alerts/open")
                        .param("severity", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(openHighAlert.getId().toString()))
                .andExpect(jsonPath("$[0].severity").value("HIGH"))
                .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    @Test
    void getOpenAlertsBySeverity_shouldRejectInvalidSeverity() throws Exception {
        mockMvc.perform(get("/api/alerts/open")
                        .param("severity", "EXTREME"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAlertById_shouldReturnAlert() throws Exception {
        Alert alert = createSavedAlert(AlertSeverity.HIGH);

        mockMvc.perform(get("/api/alerts/{id}", alert.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(alert.getId().toString()))
                .andExpect(jsonPath("$.severity").value("HIGH"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.securityEventId").value(alert.getSecurityEvent().getId().toString()))
                .andExpect(jsonPath("$.deviceName").value("Front Door Camera"));
    }

    @Test
    void getAlertById_shouldReturnNotFoundWhenMissing() throws Exception {
        UUID missingId = UUID.randomUUID();

        mockMvc.perform(get("/api/alerts/{id}", missingId))
                .andExpect(status().isNotFound());
    }

    @Test
    void acknowledgeAlert_shouldUpdateAlertStatus() throws Exception {
        Alert alert = createSavedAlert(AlertSeverity.HIGH);

        mockMvc.perform(patch("/api/alerts/{id}/acknowledge", alert.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(alert.getId().toString()))
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"))
                .andExpect(jsonPath("$.acknowledgedAt").exists());

        Alert reloadedAlert = alertRepository.findById(alert.getId()).orElseThrow();

        assert reloadedAlert.getStatus() == AlertStatus.ACKNOWLEDGED;
        assert reloadedAlert.getAcknowledgedAt() != null;
    }

    @Test
    void acknowledgeAlert_shouldReturnNotFoundWhenMissing() throws Exception {
        UUID missingId = UUID.randomUUID();

        mockMvc.perform(patch("/api/alerts/{id}/acknowledge", missingId))
                .andExpect(status().isNotFound());
    }

    @Test
    void resolveAlert_shouldUpdateAlertStatus() throws Exception {
        Alert alert = createSavedAlert(AlertSeverity.HIGH);

        mockMvc.perform(patch("/api/alerts/{id}/resolve", alert.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(alert.getId().toString()))
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedAt").exists());

        Alert reloadedAlert = alertRepository.findById(alert.getId()).orElseThrow();

        assert reloadedAlert.getStatus() == AlertStatus.RESOLVED;
        assert reloadedAlert.getResolvedAt() != null;
    }

    @Test
    void resolveAlert_shouldReturnNotFoundWhenMissing() throws Exception {
        UUID missingId = UUID.randomUUID();

        mockMvc.perform(patch("/api/alerts/{id}/resolve", missingId))
                .andExpect(status().isNotFound());
    }

    private Alert createSavedAlert(AlertSeverity alertSeverity) {
        SecurityEventSeverity eventSeverity = switch (alertSeverity) {
            case LOW -> SecurityEventSeverity.LOW;
            case MEDIUM -> SecurityEventSeverity.MEDIUM;
            case HIGH -> SecurityEventSeverity.HIGH;
            case CRITICAL -> SecurityEventSeverity.CRITICAL;
        };

        SecurityEvent event = createSavedSecurityEvent(eventSeverity);

        Alert alert = new Alert(
                event,
                alertSeverity,
                alertSeverity + " security event: " + event.getEventType(),
                "Device Front Door Camera reported " + event.getEventType()
                        + " with severity " + event.getSeverity() + "."
        );

        return alertRepository.save(alert);
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