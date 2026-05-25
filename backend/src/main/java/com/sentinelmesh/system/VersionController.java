package com.sentinelmesh.system;

import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VersionController 
{
	@Value("${spring.application.name}")
	private String applicationName;
	
	@Value("${app.version}")
	private String version;
	
	@GetMapping("/api/version")
	public ResponseEntity<Map<String, Object>> version()
	{
		return ResponseEntity.ok(Map.of(
		"application", applicationName,
		"version", version,
		"environment", "local",
		"timestamp", Instant.now().toString()
				));
	}
}
