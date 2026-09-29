package com.sentinelmesh.devices;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceRepository extends JpaRepository<Device, UUID>
{
	List<Device> findByStatus(DeviceStatus status);
	
	List<Device> findByType(DeviceType type);

	@Query("SELECT device.type FROM Device device WHERE device.id = :deviceId")
	Optional<DeviceType> findTypeById(@Param("deviceId") UUID deviceId);
}