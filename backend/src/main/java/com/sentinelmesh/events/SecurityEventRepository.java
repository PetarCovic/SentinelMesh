package com.sentinelmesh.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sentinelmesh.devices.Device;

public interface SecurityEventRepository extends JpaRepository<SecurityEvent, UUID>
{
	List<SecurityEvent> findByDevice(Device device);
	
	List<SecurityEvent> findByDeviceId(UUID deviceId);
	
	List<SecurityEvent> findTop5ByDeviceIdOrderByReceivedAtDesc(UUID deviceId);
	
	List<SecurityEvent> findByEventType(SecurityEventType eventType);
	
	List<SecurityEvent> findBySeverity(SecurityEventSeverity severity);
	
	List<SecurityEvent> findByOccurredAtBetween(Instant start, Instant end);
	
	Page<SecurityEvent> findAllByOrderByReceivedAtDesc(Pageable pageable);
}
