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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sentinelmesh.TestDatabaseCleaner;
import com.sentinelmesh.TestQueueConfig;
import com.sentinelmesh.realtime.DashboardEventBroadcaster;

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

    @MockitoBean
    private DashboardEventBroadcaster dashboardEventBroadcaster;

    @BeforeEach
    void setUp() {
        testDatabaseCleaner.clean();
    }

    @Test
    void createDevice_shouldReturnCreatedDeviceWithApiKey() throws Exception {
        String json = """
                {
                  "name": "Front Door Camera",
                  "type": "CAMERA",
                  "location": "Front Porch"
                }
                """;

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.apiKey").exists())
                .andExpect(jsonPath("$.name").value("Front Door Camera"))
                .andExpect(jsonPath("$.type").value("CAMERA"))
                .andExpect(jsonPath("$.location").value("Front Porch"))
                .andExpect(jsonPath("$.status").value("OFFLINE"));
    }

    @Test
    void createDevice_shouldReturnBadRequestWhenNameMissing() throws Exception {
        String json = """
                {
                  "type": "CAMERA",
                  "location": "Front Porch"
                }
                """;

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllDevices_shouldReturnDevices() throws Exception {
        deviceRepository.save(new Device(
                "Front Door Camera",
                DeviceType.CAMERA,
                "Front Porch"
        ));

        deviceRepository.save(new Device(
                "Garage Door Sensor",
                DeviceType.DOOR_SENSOR,
                "Garage"
        ));

        mockMvc.perform(get("/api/devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getDeviceById_shouldReturnDevice() throws Exception {
        Device device = deviceRepository.save(new Device(
                "Front Door Camera",
                DeviceType.CAMERA,
                "Front Porch"
        ));

        mockMvc.perform(get("/api/devices/{id}", device.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(device.getId().toString()))
                .andExpect(jsonPath("$.name").value("Front Door Camera"))
                .andExpect(jsonPath("$.type").value("CAMERA"))
                .andExpect(jsonPath("$.location").value("Front Porch"));
    }

    @Test
    void getDeviceById_shouldReturnNotFoundWhenMissing() throws Exception {
        UUID missingId = UUID.randomUUID();

        mockMvc.perform(get("/api/devices/{id}", missingId))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateDevice_shouldUpdateDevice() throws Exception {
        Device device = deviceRepository.save(new Device(
                "Front Door Camera",
                DeviceType.CAMERA,
                "Front Porch"
        ));

        String json = """
                {
                  "location": "Entryway"
                }
                """;

        mockMvc.perform(patch("/api/devices/{id}", device.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(device.getId().toString()))
                .andExpect(jsonPath("$.name").value("Front Door Camera"))
                .andExpect(jsonPath("$.location").value("Entryway"));
    }

    @Test
    void updateDevice_shouldReturnNotFoundWhenMissing() throws Exception {
        UUID missingId = UUID.randomUUID();

        String json = """
                {
                  "location": "Entryway"
                }
                """;

        mockMvc.perform(patch("/api/devices/{id}", missingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteDevice_shouldDeleteDevice() throws Exception {
        Device device = deviceRepository.save(new Device(
                "Front Door Camera",
                DeviceType.CAMERA,
                "Front Porch"
        ));

        mockMvc.perform(delete("/api/devices/{id}", device.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteDevice_shouldReturnNotFoundWhenMissing() throws Exception {
        UUID missingId = UUID.randomUUID();

        mockMvc.perform(delete("/api/devices/{id}", missingId))
                .andExpect(status().isNotFound());
    }
}