package com.redis.infrastructure.config;

import com.redis.notification.entity.NotificationChannel;
import com.redis.notification.entity.NotificationPriority;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.notification")
public class NotificationProperties {
    private int lowStockThreshold = 5;
    private NotificationChannel defaultChannel = NotificationChannel.EMAIL;
    private NotificationPriority defaultPriority = NotificationPriority.MEDIUM;
    private List<String> adminEmails = List.of("admin@ecommerce.com");
    private Retry retry = new Retry();

    private boolean queueEnabled = false;
    private boolean outboxEnabled = false;
    private int workerBatchSize = 50;
    private int workerThreads = 4;
    private String eventBusType = "LOCAL";
    private String redisQueueName = "notification:queue";
    private int outboxBatchSize = 100;
    private int outboxDelayMs = 1000;

    private boolean templateManagementEnabled = false;
    private boolean preferenceValidationEnabled = false;
    private boolean rateLimitingEnabled = false;
    private int defaultRateLimitPerHour = 10;
    private int defaultRateLimitPerDay = 100;
    private boolean previewEnabled = true;
    private boolean templateCacheEnabled = true;

    @Getter
    @Setter
    public static class Retry {
        private int maxAttempts = 5;
        private int initialDelayMinutes = 1;
        private int backoffMultiplier = 2;
        private int schedulerDelaySeconds = 60;
        private int batchSize = 100;
        private boolean deadLetterEnabled = true;
    }
}
