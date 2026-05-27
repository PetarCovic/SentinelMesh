package com.sentinelmesh.alerts;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, UUID>
{
	List<Alert> findByStatus(AlertStatus status);
	
	List<Alert> findBySeverity(AlertSeverity severity);
	
	List<Alert> findByStatusAndSeverity(AlertStatus status, AlertSeverity severity);
	
	boolean existsBySecurityEventId(UUID securityEventId);
}
