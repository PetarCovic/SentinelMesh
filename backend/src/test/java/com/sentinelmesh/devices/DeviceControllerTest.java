package com.sentinelmesh.devices;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.sentinelmesh.TestDatabaseCleaner;
import com.sentinelmesh.TestQueueConfig;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestQueueConfig.class)
class DeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private TestDatabaseCleaner testDatabaseCleaner;

    @BeforeEach
    void setUp() {
        testDatabaseCleaner.clean();
    }

    @Test
    void createDevice_shouldReturnCreatedDevice() throws Exception {
        String requestJson = """
                {
                  "name": "Front Door Camera",
                  "type": "CAMERA",
                  "location": "Front Porch"
                }
                """;

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Front Door Camera"))
                .andExpect(jsonPath("$.type").value("CAMERA"))
                .andExpect(jsonPath("$.location").value("Front Porch"))
                .andExpect(jsonPath("$.status").value("OFFLINE"))
                .andExpect(jsonPath("$.lastSeenAt").doesNotExist());
    }

    @Test
    void createDevice_shouldRejectMissingName() throws Exception {
        String requestJson = """
                {
                  "type": "CAMERA",
                  "location": "Front Porch"
                }
                """;

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDevice_shouldRejectInvalidDeviceType() throws Exception {
        String requestJson = """
                {
                  "name": "Invalid Device",
                  "type": "DOG",
                  "location": "Front Porch"
                }
                """;

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllDevices_shouldReturnDevices() throws Exception {
        Device device = new Device(
                "Front Door Camera",
                DeviceType.CAMERA,
                "Front Porch"
        );

        deviceRepository.save(device);

        mockMvc.perform(get("/api/devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Front Door Camera"))
                .andExpect(jsonPath("$[0].type").value("CAMERA"))
                .andExpect(jsonPath("$[0].location").value("Front Porch"));
    }

    @Test
    void getDeviceById_shouldReturnDevice() throws Exception {
        Device device = new Device(
                "Front Door Camera",
                DeviceType.CAMERA,
                "Front Porch"
        );

        Device saved = deviceRepository.save(device);

        mockMvc.perform(get("/api/devices/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.name").value("Front Door Camera"))
                .andExpect(jsonPath("$.type").value("CAMERA"))
                .andExpect(jsonPath("$.location").value("Front Porch"));
    }

    @Test
    void getDeviceById_shouldReturnNotFoundWhenDeviceDoesNotExist() throws Exception {
        UUID missingId = UUID.randomUUID();

        mockMvc.perform(get("/api/devices/{id}", missingId))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateDevice_shouldUpdateProvidedFields() throws Exception {
        Device device = new Device(
                "Front Door Camera",
                DeviceType.CAMERA,
                "Front Porch"
        );

        Device saved = deviceRepository.save(device);

        String requestJson = """
                {
                  "location": "Entryway"
                }
                """;

        mockMvc.perform(patch("/api/devices/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.name").value("Front Door Camera"))
                .andExpect(jsonPath("$.type").value("CAMERA"))
                .andExpect(jsonPath("$.location").value("Entryway"));
    }

    @Test
    void updateDevice_shouldReturnNotFoundWhenDeviceDoesNotExist() throws Exception {
        UUID missingId = UUID.randomUUID();

        String requestJson = """
                {
                  "location": "Garage"
                }
                """;

        mockMvc.perform(patch("/api/devices/{id}", missingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteDevice_shouldReturnNoContent() throws Exception {
        Device device = new Device(
                "Front Door Camera",
                DeviceType.CAMERA,
                "Front Porch"
        );

        Device saved = deviceRepository.save(device);

        mockMvc.perform(delete("/api/devices/{id}", saved.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/devices/{id}", saved.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteDevice_shouldReturnNotFoundWhenDeviceDoesNotExist() throws Exception {
        UUID missingId = UUID.randomUUID();

        mockMvc.perform(delete("/api/devices/{id}", missingId))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void createDevice_shouldReturnRawApiKeyOnce() throws Exception {
        String requestJson = """
                {
                  "name": "Backyard Camera",
                  "type": "CAMERA",
                  "location": "Backyard"
                }
                """;

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Backyard Camera"))
                .andExpect(jsonPath("$.type").value("CAMERA"))
                .andExpect(jsonPath("$.location").value("Backyard"))
                .andExpect(jsonPath("$.status").value("OFFLINE"))
                .andExpect(jsonPath("$.apiKey").exists())
                .andExpect(jsonPath("$.apiKey").value(org.hamcrest.Matchers.startsWith("sm_live_")));
    }

    @Test
    void createDevice_shouldStoreApiKeyHashButNotRawApiKey() throws Exception {
        String requestJson = """
                {
                  "name": "Backyard Camera",
                  "type": "CAMERA",
                  "location": "Backyard"
                }
                """;

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.apiKey").exists());

        Device savedDevice = deviceRepository.findAll().get(0);

        assertNotNull(savedDevice.getApiKeyHash());
        assertFalse(savedDevice.getApiKeyHash().isBlank());
        assertFalse(savedDevice.getApiKeyHash().startsWith("sm_live_"));
        assertEquals(64, savedDevice.getApiKeyHash().length());
    }

    @Test
    void getAllDevices_shouldNotExposeApiKeyOrApiKeyHash() throws Exception {
        Device device = new Device(
                "Front Door Camera",
                DeviceType.CAMERA,
                "Front Porch"
        );
        device.setApiKeyHash("fake_hash_for_test");
        deviceRepository.save(device);

        mockMvc.perform(get("/api/devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].name").value("Front Door Camera"))
                .andExpect(jsonPath("$[0]", not(hasKey("apiKey"))))
                .andExpect(jsonPath("$[0]", not(hasKey("apiKeyHash"))));
    }

    @Test
    void getDeviceById_shouldNotExposeApiKeyOrApiKeyHash() throws Exception {
        Device device = new Device(
                "Front Door Camera",
                DeviceType.CAMERA,
                "Front Porch"
        );
        device.setApiKeyHash("fake_hash_for_test");

        Device savedDevice = deviceRepository.save(device);

        mockMvc.perform(get("/api/devices/{id}", savedDevice.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedDevice.getId().toString()))
                .andExpect(jsonPath("$.name").value("Front Door Camera"))
                .andExpect(jsonPath("$", not(hasKey("apiKey"))))
                .andExpect(jsonPath("$", not(hasKey("apiKeyHash"))));
    }

    @Test
    void updateDevice_shouldNotExposeApiKeyOrApiKeyHash() throws Exception {
        Device device = new Device(
                "Backyard Camera",
                DeviceType.CAMERA,
                "Backyard"
        );
        device.setApiKeyHash("fake_hash_for_test");

        Device savedDevice = deviceRepository.save(device);

        String requestJson = """
                {
                  "location": "Backyard Patio"
                }
                """;

        mockMvc.perform(patch("/api/devices/{id}", savedDevice.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedDevice.getId().toString()))
                .andExpect(jsonPath("$.location").value("Backyard Patio"))
                .andExpect(jsonPath("$", not(hasKey("apiKey"))))
                .andExpect(jsonPath("$", not(hasKey("apiKeyHash"))));
    }
}