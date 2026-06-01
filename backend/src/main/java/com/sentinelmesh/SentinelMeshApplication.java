package com.sentinelmesh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.sentinelmesh.async.EventProcessingProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(EventProcessingProperties.class)
public class SentinelMeshApplication 
{

	public static void main(String[] args) 
	{
		SpringApplication.run(SentinelMeshApplication.class, args);
	}

}
