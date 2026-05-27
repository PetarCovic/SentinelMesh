package com.sentinelmesh.devices;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.sentinelmesh.TestDatabaseCleaner;

@SpringBootTest
@ActiveProfiles("test")
class DeviceStatusMonitorTest {

    @Autowired
    private DeviceStatusMonitor deviceStatusMonitor;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private TestDatabaseCleaner testDatabaseCleaner;

    @BeforeEach
    void setUp() {
        testDatabaseCleaner.clean();
    }

    @Test
    void markStaleDevicesOffline_shouldMarkOldOnlineDeviceOffline() {
        Device device = new Device(
                "Garage Camera",
                DeviceType.CAMERA,
                "Garage"
        );

        device.setStatus(DeviceStatus.ONLINE);
        device.setLastSeenAt(Instant.now().minusSeconds(60));

        Device savedDevice = deviceRepository.save(device);

        deviceStatusMonitor.markStaleDevicesOffline();

        Device reloadedDevice = deviceRepository.findById(savedDevice.getId())
                .orElseThrow();

        assertEquals(DeviceStatus.OFFLINE, reloadedDevice.getStatus());
    }

    @Test
    void markStaleDevicesOffline_shouldKeepRecentlySeenDeviceOnline() {
        Device device = new Device(
                "Front Door Camera",
                DeviceType.CAMERA,
                "Front Porch"
        );

        device.setStatus(DeviceStatus.ONLINE);
        device.setLastSeenAt(Instant.now());

        Device savedDevice = deviceRepository.save(device);

        deviceStatusMonitor.markStaleDevicesOffline();

        Device reloadedDevice = deviceRepository.findById(savedDevice.getId())
                .orElseThrow();

        assertEquals(DeviceStatus.ONLINE, reloadedDevice.getStatus());
    }

    @Test
    void markStaleDevicesOffline_shouldMarkOnlineDeviceWithNullLastSeenOffline() {
        Device device = new Device(
                "Backyard Camera",
                DeviceType.CAMERA,
                "Backyard"
        );

        device.setStatus(DeviceStatus.ONLINE);
        device.setLastSeenAt(null);

        Device savedDevice = deviceRepository.save(device);

        deviceStatusMonitor.markStaleDevicesOffline();

        Device reloadedDevice = deviceRepository.findById(savedDevice.getId())
                .orElseThrow();

        assertEquals(DeviceStatus.OFFLINE, reloadedDevice.getStatus());
    }

    @Test
    void markStaleDevicesOffline_shouldNotChangeAlreadyOfflineDevice() {
        Device device = new Device(
                "Offline Camera",
                DeviceType.CAMERA,
                "Basement"
        );

        device.setStatus(DeviceStatus.OFFLINE);
        device.setLastSeenAt(Instant.now().minusSeconds(120));

        Device savedDevice = deviceRepository.save(device);

        deviceStatusMonitor.markStaleDevicesOffline();

        Device reloadedDevice = deviceRepository.findById(savedDevice.getId())
                .orElseThrow();

        assertEquals(DeviceStatus.OFFLINE, reloadedDevice.getStatus());
    }
}