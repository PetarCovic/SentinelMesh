package com.sentinelmesh.clips;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoClipRepository extends JpaRepository<VideoClip, UUID>
{
	Optional<VideoClip> findByEventId(UUID eventId);
	
	boolean existsByEventId(UUID eventId);
	
	List<VideoClip> findByDeviceIdOrderByCreatedAtDesc(UUID deviceId);
	
	List<VideoClip> findByEventIdIn(Collection<UUID> eventIds);
}
