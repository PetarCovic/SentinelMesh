package com.sentinelmesh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SentinelMeshApplication {

	public static void main(String[] args) {
		SpringApplication.run(SentinelMeshApplication.class, args);
	}

}
