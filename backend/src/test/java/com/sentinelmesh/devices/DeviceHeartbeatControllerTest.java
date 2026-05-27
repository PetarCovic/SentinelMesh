package com.sentinelmesh.devices;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.sentinelmesh.TestDatabaseCleaner;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeviceHeartbeatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private ApiKeyHashService apiKeyHashService;

    private String rawApiKey;

    @Autowired
    private TestDatabaseCleaner testDatabaseCleaner;

    @BeforeEach
    void setUp() 
    {
        testDatabaseCleaner.clean();
 
        rawApiKey = "sm_live_controller_test_key";
    }

    @Test
    void recordHeartbeat_shouldReturnOkAndSetDeviceOnline() throws Exception {
        Device savedDevice = createSavedDeviceWithApiKey();

        String requestJson = """
                {
                  "status": "ONLINE"
                }
                """;

        mockMvc.perform(post("/api/devices/{id}/heartbeat", savedDevice.getId())
                        .header("X-Device-Api-Key", rawApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedDevice.getId().toString()))
                .andExpect(jsonPath("$.name").value("Garage Camera"))
                .andExpect(jsonPath("$.status").value("ONLINE"))
                .andExpect(jsonPath("$.lastSeenAt").exists())
                .andExpect(jsonPath("$", not(hasKey("apiKey"))))
                .andExpect(jsonPath("$", not(hasKey("apiKeyHash"))));
    }

    @Test
    void recordHeartbeat_shouldRejectWrongApiKey() throws Exception {
        Device savedDevice = createSavedDeviceWithApiKey();

        String requestJson = """
                {
                  "status": "ONLINE"
                }
                """;

        mockMvc.perform(post("/api/devices/{id}/heartbeat", savedDevice.getId())
                        .header("X-Device-Api-Key", "wrong_key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void recordHeartbeat_shouldReturnNotFoundForMissingDevice() throws Exception {
        String requestJson = """
                {
                  "status": "ONLINE"
                }
                """;

        mockMvc.perform(post("/api/devices/{id}/heartbeat", "00000000-0000-0000-0000-000000000000")
                        .header("X-Device-Api-Key", rawApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound());
    }

    @Test
    void recordHeartbeat_shouldRejectMissingApiKeyHeader() throws Exception {
        Device savedDevice = createSavedDeviceWithApiKey();

        String requestJson = """
                {
                  "status": "ONLINE"
                }
                """;

        mockMvc.perform(post("/api/devices/{id}/heartbeat", savedDevice.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recordHeartbeat_shouldRejectMissingStatus() throws Exception {
        Device savedDevice = createSavedDeviceWithApiKey();

        String requestJson = """
                {
                }
                """;

        mockMvc.perform(post("/api/devices/{id}/heartbeat", savedDevice.getId())
                        .header("X-Device-Api-Key", rawApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recordHeartbeat_shouldRejectInvalidStatus() throws Exception {
        Device savedDevice = createSavedDeviceWithApiKey();

        String requestJson = """
                {
                  "status": "BROKEN"
                }
                """;

        mockMvc.perform(post("/api/devices/{id}/heartbeat", savedDevice.getId())
                        .header("X-Device-Api-Key", rawApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    private Device createSavedDeviceWithApiKey() {
        Device device = new Device(
                "Garage Camera",
                DeviceType.CAMERA,
                "Garage"
        );

        device.setApiKeyHash(apiKeyHashService.hash(rawApiKey));

        return deviceRepository.save(device);
    }
}