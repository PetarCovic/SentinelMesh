package com.sentinelmesh.devices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sentinelmesh.TestDatabaseCleaner;
import com.sentinelmesh.TestQueueConfig;
import com.sentinelmesh.devices.requests.CreateDeviceRequest;
import com.sentinelmesh.devices.requests.UpdateDeviceRequest;
import com.sentinelmesh.exceptions.DeviceNotFoundException;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestQueueConfig.class)
class DeviceServiceTest {

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private TestDatabaseCleaner testDatabaseCleaner;

    @BeforeEach
    void setUp() {
        testDatabaseCleaner.clean();
    }

    @Test
    void createDevice_shouldSaveDevice() {
        CreateDeviceRequest request = new CreateDeviceRequest();
        request.setName("Front Door Camera");
        request.setType(DeviceType.CAMERA);
        request.setLocation("Front Porch");

        CreateDeviceResponse response = deviceService.createDevice(request);

        assertNotNull(response.getId());
        assertEquals("Front Door Camera", response.getName());
        assertEquals(DeviceType.CAMERA, response.getType());
        assertEquals("Front Porch", response.getLocation());
        assertEquals(DeviceStatus.OFFLINE, response.getStatus());
        assertNull(response.getLastSeenAt());
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getUpdatedAt());

        assertEquals(1, deviceRepository.count());
    }

    @Test
    void getAllDevices_shouldReturnSavedDevices() {
        CreateDeviceRequest request1 = new CreateDeviceRequest();
        request1.setName("Front Door Camera");
        request1.setType(DeviceType.CAMERA);
        request1.setLocation("Front Porch");

        CreateDeviceRequest request2 = new CreateDeviceRequest();
        request2.setName("Garage Door Sensor");
        request2.setType(DeviceType.DOOR_SENSOR);
        request2.setLocation("Garage");

        deviceService.createDevice(request1);
        deviceService.createDevice(request2);

        var devices = deviceService.getAllDevices();

        assertEquals(2, devices.size());
    }

    @Test
    void getDeviceById_shouldReturnDeviceWhenItExists() {
        CreateDeviceRequest request = new CreateDeviceRequest();
        request.setName("Front Door Camera");
        request.setType(DeviceType.CAMERA);
        request.setLocation("Front Porch");

        CreateDeviceResponse created = deviceService.createDevice(request);

        DeviceResponse found = deviceService.getDeviceById(created.getId());

        assertEquals(created.getId(), found.getId());
        assertEquals("Front Door Camera", found.getName());
        assertEquals(DeviceType.CAMERA, found.getType());
    }

    @Test
    void getDeviceById_shouldThrowWhenDeviceDoesNotExist() {
        UUID missingId = UUID.randomUUID();

        assertThrows(DeviceNotFoundException.class, () -> {
            deviceService.getDeviceById(missingId);
        });
    }

    @Test
    void updateDevice_shouldUpdateOnlyProvidedFields() {
        CreateDeviceRequest createRequest = new CreateDeviceRequest();
        createRequest.setName("Front Door Camera");
        createRequest.setType(DeviceType.CAMERA);
        createRequest.setLocation("Front Porch");

        CreateDeviceResponse created = deviceService.createDevice(createRequest);

        UpdateDeviceRequest updateRequest = new UpdateDeviceRequest();
        updateRequest.setLocation("Entryway");

        DeviceResponse updated = deviceService.updateDevice(created.getId(), updateRequest);

        assertEquals(created.getId(), updated.getId());
        assertEquals("Front Door Camera", updated.getName());
        assertEquals(DeviceType.CAMERA, updated.getType());
        assertEquals("Entryway", updated.getLocation());
        assertEquals(DeviceStatus.OFFLINE, updated.getStatus());
    }

    @Test
    void updateDevice_shouldThrowWhenDeviceDoesNotExist() {
        UUID missingId = UUID.randomUUID();

        UpdateDeviceRequest updateRequest = new UpdateDeviceRequest();
        updateRequest.setLocation("Garage");

        assertThrows(DeviceNotFoundException.class, () -> {
            deviceService.updateDevice(missingId, updateRequest);
        });
    }

    @Test
    void deleteDevice_shouldRemoveDevice() {
        CreateDeviceRequest request = new CreateDeviceRequest();
        request.setName("Front Door Camera");
        request.setType(DeviceType.CAMERA);
        request.setLocation("Front Porch");

        CreateDeviceResponse created = deviceService.createDevice(request);

        assertEquals(1, deviceRepository.count());

        deviceService.deleteDevice(created.getId());

        assertEquals(0, deviceRepository.count());
        assertFalse(deviceRepository.findById(created.getId()).isPresent());
    }

    @Test
    void deleteDevice_shouldThrowWhenDeviceDoesNotExist() {
        UUID missingId = UUID.randomUUID();

        assertThrows(DeviceNotFoundException.class, () -> {
            deviceService.deleteDevice(missingId);
        });
    }
}