package com.sentinelmesh.snapshots;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SnapshotRepository extends JpaRepository<Snapshot, UUID>
{
	Optional<Snapshot> findByEventId(UUID eventId);
	
	boolean existsByEventId(UUID eventId);
	
	List<Snapshot> findByDeviceIdOrderByCreatedAtDesc(UUID deviceId);
	
	List<Snapshot> findByEventIdIn(Collection<UUID> eventIds);
}