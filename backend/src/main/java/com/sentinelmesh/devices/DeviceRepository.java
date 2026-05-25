package com.sentinelmesh.devices;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<Device, UUID>
{
	List<Device> findByStatus(DeviceStatus status);
	
	List<Device> findByType(DeviceType type);
}