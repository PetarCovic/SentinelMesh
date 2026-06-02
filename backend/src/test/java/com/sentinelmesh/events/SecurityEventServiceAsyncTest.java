package com.sentinelmesh.events;

import static org.hamcrest.CoreMatchers.any;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sentinelmesh.TestDatabaseCleaner;
import com.sentinelmesh.TestQueueConfig;
import com.sentinelmesh.devices.CreateDeviceResponse;
import com.sentinelmesh.devices.DeviceService;
import com.sentinelmesh.devices.DeviceType;
import com.sentinelmesh.devices.requests.CreateDeviceRequest;
import com.sentinelmesh.realtime.DashboardEventBroadcaster;
import com.sentinelmesh.realtime.DashboardEventType;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestQueueConfig.class)
class SecurityEventServiceAsyncTest {

    @Autowired
    private SecurityEventService securityEventService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private SecurityEventRepository securityEventRepository;

    @Autowired
    private TestQueueConfig.InMemoryEventProcessingQueue eventProcessingQueue;

    @Autowired
    private TestDatabaseCleaner testDatabaseCleaner;
    
    @MockitoBean
    private DashboardEventBroadcaster dashboardEventBroadcaster;

    @BeforeEach
    void setUp() {
        testDatabaseCleaner.clean();
        eventProcessingQueue.clear();
    }

    @Test
    void createEvent_shouldSaveEventAndEnqueueEventId() {
        CreateDeviceResponse device = createDevice();

        CreateSecurityEventRequest request = new CreateSecurityEventRequest();
        request.setEventType(SecurityEventType.PERSON_DETECTED);
        request.setSeverity(SecurityEventSeverity.HIGH);
        request.setConfidence(0.93);
        request.setMetadataJson("{\"source\":\"async-test\"}");

        SecurityEventResponse response = securityEventService.createEvent(
                device.getId(),
                device.getApiKey(),
                request
        );

        assertNotNull(response.getId());
        assertEquals(1, securityEventRepository.count());

        assertEquals(1, eventProcessingQueue.getQueuedEventIds().size());
        assertEquals(response.getId(), eventProcessingQueue.getQueuedEventIds().get(0));
        
        verify(dashboardEventBroadcaster).broadcast(
                eq(DashboardEventType.SECURITY_EVENT_CREATED),
                any()
        );
    }

    private CreateDeviceResponse createDevice() {
        CreateDeviceRequest request = new CreateDeviceRequest();
        request.setName("Async Test Camera");
        request.setType(DeviceType.CAMERA);
        request.setLocation("Garage");

        return deviceService.createDevice(request);
    }
}