package com.sentinelmesh.devices;

import static org.hamcrest.Matchers.hasSize;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeviceRepository deviceRepository;

    @BeforeEach
    void setUp() {
        deviceRepository.deleteAll();
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
}