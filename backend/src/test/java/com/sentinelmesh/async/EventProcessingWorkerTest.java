package com.sentinelmesh.async;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sentinelmesh.devices.Device;
import com.sentinelmesh.devices.DeviceType;
import com.sentinelmesh.events.SecurityEvent;
import com.sentinelmesh.events.SecurityEventRepository;
import com.sentinelmesh.events.SecurityEventSeverity;
import com.sentinelmesh.events.SecurityEventType;
import com.sentinelmesh.rules.RuleEvaluationService;

class EventProcessingWorkerTest {

    private EventProcessingQueue eventProcessingQueue;
    private SecurityEventRepository securityEventRepository;
    private RuleEvaluationService ruleEvaluationService;
    private EventProcessingProperties properties;
    private EventProcessingWorker worker;

    @BeforeEach
    void setUp() {
        eventProcessingQueue = mock(EventProcessingQueue.class);
        securityEventRepository = mock(SecurityEventRepository.class);
        ruleEvaluationService = mock(RuleEvaluationService.class);
        properties = new EventProcessingProperties();

        worker = new EventProcessingWorker(
                eventProcessingQueue,
                securityEventRepository,
                ruleEvaluationService,
                properties
        );
    }

    @Test
    void processEvent_shouldEvaluateRuleWhenEventExists() {
        UUID eventId = UUID.randomUUID();

        Device device = new Device(
                "Front Door Camera",
                DeviceType.CAMERA,
                "Front Porch"
        );

        SecurityEvent event = new SecurityEvent(
                device,
                SecurityEventType.PERSON_DETECTED,
                SecurityEventSeverity.HIGH,
                0.93,
                Instant.now(),
                "{\"source\":\"worker-test\"}"
        );

        when(securityEventRepository.findById(eventId)).thenReturn(Optional.of(event));

        worker.processEvent(eventId);

        verify(securityEventRepository).findById(eventId);
        verify(ruleEvaluationService).evaluate(event);
    }

    @Test
    void processEvent_shouldNotEvaluateRuleWhenEventDoesNotExist() {
        UUID missingEventId = UUID.randomUUID();

        when(securityEventRepository.findById(missingEventId)).thenReturn(Optional.empty());

        worker.processEvent(missingEventId);

        verify(securityEventRepository).findById(missingEventId);
        verifyNoInteractions(ruleEvaluationService);
    }
}