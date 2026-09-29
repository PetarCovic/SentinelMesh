package com.sentinelmesh.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcAsyncConfig implements WebMvcConfigurer
{
	private final ThreadPoolTaskExecutor liveStreamExecutor;

	public WebMvcAsyncConfig(ThreadPoolTaskExecutor liveStreamExecutor)
	{
		this.liveStreamExecutor = liveStreamExecutor;
	}

	@Bean
	public static ThreadPoolTaskExecutor liveStreamExecutor()
	{
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

		executor.setCorePoolSize(4);
		executor.setMaxPoolSize(16);
		executor.setQueueCapacity(0);
		executor.setThreadNamePrefix("live-stream-");

		return executor;
	}

	@Override
	public void configureAsyncSupport(AsyncSupportConfigurer configurer)
	{
		configurer.setTaskExecutor(liveStreamExecutor);
		configurer.setDefaultTimeout(0);
	}
}