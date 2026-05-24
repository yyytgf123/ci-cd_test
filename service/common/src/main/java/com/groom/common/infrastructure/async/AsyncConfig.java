package com.groom.common.infrastructure.async;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

	/**
	 * Domain Event 처리 전용
	 */
	@Bean(name = "eventExecutor")
	public Executor eventExecutor() {
		return createExecutor(
			"event-",
			2,
			4,
			200
		);
	}

	/**
	 * 외부 I/O (Redis, Feign 등)
	 */
	@Bean(name = "ioExecutor")
	public Executor ioExecutor() {
		return createExecutor(
			"io-",
			4,
			8,
			500
		);
	}

	private Executor createExecutor(
		String prefix,
		int core,
		int max,
		int queue
	) {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(core);
		executor.setMaxPoolSize(max);
		executor.setQueueCapacity(queue);
		executor.setThreadNamePrefix(prefix);
		executor.initialize();
		return executor;
	}
}
