package com.redis.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Custom configured thread pool executor for processing notification events asynchronously.
     * Core Pool Size: 10 threads, sufficient to keep active under normal system loads.
     * Max Pool Size: 50 threads, allowing expansion during peak marketing or event campaigns.
     * Queue Capacity: 10,000 requests, buffering items in memory securely to prevent drop-off.
     */
    @Bean(name = "notificationAsyncExecutor")
    public Executor notificationAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(10000);
        executor.setThreadNamePrefix("notification-exec-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "auditAsyncExecutor")
    public Executor auditAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(5000);
        executor.setThreadNamePrefix("audit-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
