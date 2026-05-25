package com.sentinelmesh.devices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class DeviceRepositoryTest {

    @Autowired
    private DeviceRepository deviceRepository;

    @Test
    void save_shouldPersistDevice() {
        Device device = new Device(
                "Front Door Camera",
                DeviceType.CAMERA,
                "Front Porch"
        );

        Device saved = deviceRepository.save(device);

        assertNotNull(saved.getId());
        assertEquals("Front Door Camera", saved.getName());
        assertEquals(DeviceType.CAMERA, saved.getType());
        assertEquals("Front Porch", saved.getLocation());
        assertEquals(DeviceStatus.OFFLINE, saved.getStatus());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void findByStatus_shouldReturnMatchingDevices() {
        Device camera = new Device(
                "Front Door Camera",
                DeviceType.CAMERA,
                "Front Porch"
        );

        Device sensor = new Device(
                "Garage Door Sensor",
                DeviceType.DOOR_SENSOR,
                "Garage"
        );

        deviceRepository.save(camera);
        deviceRepository.save(sensor);

        var offlineDevices = deviceRepository.findByStatus(DeviceStatus.OFFLINE);

        assertEquals(2, offlineDevices.size());
    }

    @Test
    void findByType_shouldReturnMatchingDevices() {
        Device camera = new Device(
                "Front Door Camera",
                DeviceType.CAMERA,
                "Front Porch"
        );

        Device sensor = new Device(
                "Garage Door Sensor",
                DeviceType.DOOR_SENSOR,
                "Garage"
        );

        deviceRepository.save(camera);
        deviceRepository.save(sensor);

        var cameras = deviceRepository.findByType(DeviceType.CAMERA);

        assertEquals(1, cameras.size());
        assertEquals("Front Door Camera", cameras.get(0).getName());
    }

    @Test
    void delete_shouldRemoveDevice() {
        Device device = new Device(
                "Front Door Camera",
                DeviceType.CAMERA,
                "Front Porch"
        );

        Device saved = deviceRepository.save(device);

        deviceRepository.delete(saved);

        assertFalse(deviceRepository.findById(saved.getId()).isPresent());
    }
}