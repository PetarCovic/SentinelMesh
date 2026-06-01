package com.sentinelmesh.events;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.sentinelmesh.TestDatabaseCleaner;
import com.sentinelmesh.TestQueueConfig;
import com.sentinelmesh.devices.ApiKeyHashService;
import com.sentinelmesh.devices.Device;
import com.sentinelmesh.devices.DeviceRepository;
import com.sentinelmesh.devices.DeviceType;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestQueueConfig.class)
class SecurityEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
    
        rawApiKey = "sm_redacted_rotated";
    }

    @Test
    void createEvent_shouldReturnCreatedEvent() throws Exception {
        Device savedDevice = createSavedDeviceWithApiKey();

        String requestJson = """
                {
                  "eventType": "MOTION_DETECTED",
                  "severity": "MEDIUM",
                  "confidence": 0.87,
                  "metadataJson": "{\\"zone\\":\\"front_porch\\",\\"motionArea\\":0.42}"
                }
                """;

        mockMvc.perform(post("/api/devices/{deviceId}/events", savedDevice.getId())
                        .header("X-Device-Api-Key", rawApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.deviceId").value(savedDevice.getId().toString()))
                .andExpect(jsonPath("$.deviceName").value("Front Door Camera"))
                .andExpect(jsonPath("$.eventType").value("MOTION_DETECTED"))
                .andExpect(jsonPath("$.severity").value("MEDIUM"))
                .andExpect(jsonPath("$.confidence").value(0.87))
                .andExpect(jsonPath("$.occurredAt").exists())
                .andExpect(jsonPath("$.receivedAt").exists())
                .andExpect(jsonPath("$.metadataJson").exists())
                .andExpect(jsonPath("$", not(hasKey("apiKey"))))
                .andExpect(jsonPath("$", not(hasKey("apiKeyHash"))));
    }

    @Test
    void createEvent_shouldUseProvidedOccurredAt() throws Exception {
        Device savedDevice = createSavedDeviceWithApiKey();

        String requestJson = """
                {
                  "eventType": "PERSON_DETECTED",
                  "severity": "HIGH",
                  "confidence": 0.95,
                  "occurredAt": "2026-05-26T12:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/devices/{deviceId}/events", savedDevice.getId())
                        .header("X-Device-Api-Key", rawApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.occurredAt").value("2026-05-26T12:00:00Z"));
    }

    @Test
    void createEvent_shouldRejectWrongApiKey() throws Exception {
        Device savedDevice = createSavedDeviceWithApiKey();

        String requestJson = """
                {
                  "eventType": "MOTION_DETECTED",
                  "severity": "MEDIUM",
                  "confidence": 0.87
                }
                """;

        mockMvc.perform(post("/api/devices/{deviceId}/events", savedDevice.getId())
                        .header("X-Device-Api-Key", "wrong_key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createEvent_shouldReturnNotFoundWhenDeviceDoesNotExist() throws Exception {
        UUID missingDeviceId = UUID.randomUUID();

        String requestJson = """
                {
                  "eventType": "MOTION_DETECTED",
                  "severity": "MEDIUM",
                  "confidence": 0.87
                }
                """;

        mockMvc.perform(post("/api/devices/{deviceId}/events", missingDeviceId)
                        .header("X-Device-Api-Key", rawApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound());
    }

    @Test
    void createEvent_shouldRejectMissingApiKeyHeader() throws Exception {
        Device savedDevice = createSavedDeviceWithApiKey();

        String requestJson = """
                {
                  "eventType": "MOTION_DETECTED",
                  "severity": "MEDIUM",
                  "confidence": 0.87
                }
                """;

        mockMvc.perform(post("/api/devices/{deviceId}/events", savedDevice.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEvent_shouldRejectMissingEventType() throws Exception {
        Device savedDevice = createSavedDeviceWithApiKey();

        String requestJson = """
                {
                  "severity": "MEDIUM",
                  "confidence": 0.87
                }
                """;

        mockMvc.perform(post("/api/devices/{deviceId}/events", savedDevice.getId())
                        .header("X-Device-Api-Key", rawApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEvent_shouldRejectMissingSeverity() throws Exception {
        Device savedDevice = createSavedDeviceWithApiKey();

        String requestJson = """
                {
                  "eventType": "MOTION_DETECTED",
                  "confidence": 0.87
                }
                """;

        mockMvc.perform(post("/api/devices/{deviceId}/events", savedDevice.getId())
                        .header("X-Device-Api-Key", rawApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEvent_shouldRejectInvalidEventType() throws Exception {
        Device savedDevice = createSavedDeviceWithApiKey();

        String requestJson = """
                {
                  "eventType": "DOG_DETECTED",
                  "severity": "MEDIUM",
                  "confidence": 0.87
                }
                """;

        mockMvc.perform(post("/api/devices/{deviceId}/events", savedDevice.getId())
                        .header("X-Device-Api-Key", rawApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEvent_shouldRejectInvalidSeverity() throws Exception {
        Device savedDevice = createSavedDeviceWithApiKey();

        String requestJson = """
                {
                  "eventType": "MOTION_DETECTED",
                  "severity": "EXTREME",
                  "confidence": 0.87
                }
                """;

        mockMvc.perform(post("/api/devices/{deviceId}/events", savedDevice.getId())
                        .header("X-Device-Api-Key", rawApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEvent_shouldRejectConfidenceBelowZero() throws Exception {
        Device savedDevice = createSavedDeviceWithApiKey();

        String requestJson = """
                {
                  "eventType": "MOTION_DETECTED",
                  "severity": "MEDIUM",
                  "confidence": -0.1
                }
                """;

        mockMvc.perform(post("/api/devices/{deviceId}/events", savedDevice.getId())
                        .header("X-Device-Api-Key", rawApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEvent_shouldRejectConfidenceAboveOne() throws Exception {
        Device savedDevice = createSavedDeviceWithApiKey();

        String requestJson = """
                {
                  "eventType": "MOTION_DETECTED",
                  "severity": "MEDIUM",
                  "confidence": 1.1
                }
                """;

        mockMvc.perform(post("/api/devices/{deviceId}/events", savedDevice.getId())
                        .header("X-Device-Api-Key", rawApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllEvents_shouldReturnEvents() throws Exception {
        Device savedDevice = createSavedDeviceWithApiKey();

        SecurityEvent event = new SecurityEvent(
                savedDevice,
                SecurityEventType.MOTION_DETECTED,
                SecurityEventSeverity.MEDIUM,
                0.87,
                Instant.now(),
                null
        );

        securityEventRepository.save(event);

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].deviceId").value(savedDevice.getId().toString()))
                .andExpect(jsonPath("$[0].deviceName").value("Front Door Camera"))
                .andExpect(jsonPath("$[0].eventType").value("MOTION_DETECTED"))
                .andExpect(jsonPath("$[0].severity").value("MEDIUM"));
    }

    @Test
    void getEventById_shouldReturnEvent() throws Exception {
        Device savedDevice = createSavedDeviceWithApiKey();

        SecurityEvent event = new SecurityEvent(
                savedDevice,
                SecurityEventType.PERSON_DETECTED,
                SecurityEventSeverity.HIGH,
                0.95,
                Instant.now(),
                null
        );

        SecurityEvent savedEvent = securityEventRepository.save(event);

        mockMvc.perform(get("/api/events/{id}", savedEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedEvent.getId().toString()))
                .andExpect(jsonPath("$.deviceId").value(savedDevice.getId().toString()))
                .andExpect(jsonPath("$.eventType").value("PERSON_DETECTED"))
                .andExpect(jsonPath("$.severity").value("HIGH"));
    }

    @Test
    void getEventById_shouldReturnNotFoundWhenMissing() throws Exception {
        UUID missingEventId = UUID.randomUUID();

        mockMvc.perform(get("/api/events/{id}", missingEventId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getEventsByDevice_shouldReturnOnlyDeviceEvents() throws Exception {
        Device device1 = createSavedDeviceWithApiKey(
                "Front Door Camera",
                "Front Porch",
                rawApiKey
        );

        Device device2 = createSavedDeviceWithApiKey(
                "Garage Camera",
                "Garage",
                "sm_redacted_rotated"
        );

        securityEventRepository.save(new SecurityEvent(
                device1,
                SecurityEventType.MOTION_DETECTED,
                SecurityEventSeverity.MEDIUM,
                0.80,
                Instant.now(),
                null
        ));

        securityEventRepository.save(new SecurityEvent(
                device2,
                SecurityEventType.DOOR_OPENED,
                SecurityEventSeverity.LOW,
                null,
                Instant.now(),
                null
        ));

        mockMvc.perform(get("/api/devices/{deviceId}/events", device1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].deviceId").value(device1.getId().toString()))
                .andExpect(jsonPath("$[0].eventType").value("MOTION_DETECTED"));
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