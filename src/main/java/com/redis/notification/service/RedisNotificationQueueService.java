package com.redis.notification.service;

import com.redis.infrastructure.config.NotificationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
public class RedisNotificationQueueService implements NotificationQueueService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final NotificationProperties properties;
    private final ConcurrentLinkedQueue<Long> localQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Long> localRetryQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Long> localDlq = new ConcurrentLinkedQueue<>();

    private boolean useLocalFallback = false;

    @Autowired(required = false)
    public RedisNotificationQueueService(RedisTemplate<String, Object> redisTemplate, NotificationProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        if (redisTemplate == null) {
            log.warn("RedisTemplate not found. Falling back to local in-memory queue.");
            this.useLocalFallback = true;
        }
    }

    @Scheduled(fixedDelay = 30000)
    public void attemptRecovery() {
        if (!useLocalFallback) {
            return;
        }
        if (redisTemplate == null) {
            return;
        }
        log.info("Checking Redis availability for queue service recovery...");
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            log.info("Redis connection is restored! Draining local queues to Redis...");
            drainLocalToRedis();
            this.useLocalFallback = false;
        } catch (Exception e) {
            log.warn("Redis connection check failed, remaining in fallback mode: {}", e.getMessage());
        }
    }

    private void drainLocalToRedis() {
        Long id;
        int count = 0;
        while ((id = localQueue.poll()) != null) {
            redisTemplate.opsForList().rightPush(properties.getRedisQueueName(), id.toString());
            count++;
        }
        while ((id = localRetryQueue.poll()) != null) {
            redisTemplate.opsForList().rightPush(properties.getRedisQueueName() + ":retry", id.toString());
            count++;
        }
        while ((id = localDlq.poll()) != null) {
            redisTemplate.opsForList().rightPush(properties.getRedisQueueName() + ":dlq", id.toString());
            count++;
        }
        log.info("Successfully drained {} local queue item(s) back to Redis.", count);
    }

    @Override
    public void enqueue(Long notificationId) {
        enqueue(properties.getRedisQueueName(), notificationId);
    }

    @Override
    public void enqueue(String queueName, Long notificationId) {
        if (useLocalFallback) {
            log.info("[LOCAL QUEUE] Enqueued notification ID: {} to queue: {}", notificationId, queueName);
            getLocalQueueByName(queueName).add(notificationId);
            return;
        }
        try {
            log.info("OBSERVABILITY - QUEUE_ENQUEUED: notificationId={}, queue={}", notificationId, queueName);
            redisTemplate.opsForList().rightPush(queueName, notificationId.toString());
        } catch (Exception e) {
            log.warn("Redis connection failed during enqueue. Falling back to local in-memory queue. Error: {}", e.getMessage());
            this.useLocalFallback = true;
            getLocalQueueByName(queueName).add(notificationId);
        }
    }

    @Override
    public Long dequeue() {
        return dequeue(properties.getRedisQueueName());
    }

    @Override
    public Long dequeue(String queueName) {
        if (useLocalFallback) {
            Long id = getLocalQueueByName(queueName).poll();
            if (id != null) {
                log.info("[LOCAL QUEUE] Dequeued notification ID: {} from queue: {}", id, queueName);
            }
            return id;
        }
        try {
            Object val = redisTemplate.opsForList().leftPop(queueName);
            if (val != null) {
                Long id = Long.valueOf(val.toString());
                log.info("OBSERVABILITY - QUEUE_CONSUMED: notificationId={}, queue={}", id, queueName);
                return id;
            }
            return null;
        } catch (Exception e) {
            log.warn("Redis connection failed during dequeue. Falling back to local in-memory queue. Error: {}", e.getMessage());
            this.useLocalFallback = true;
            return getLocalQueueByName(queueName).poll();
        }
    }

    @Override
    public void acknowledge(Long notificationId) {
        log.info("Acknowledged notification ID: {}", notificationId);
    }

    @Override
    public void requeue(Long notificationId) {
        log.info("OBSERVABILITY - QUEUE_RETRY: notificationId={}", notificationId);
        enqueue(properties.getRedisQueueName(), notificationId);
    }

    private ConcurrentLinkedQueue<Long> getLocalQueueByName(String queueName) {
        if (queueName.contains("retry")) {
            return localRetryQueue;
        } else if (queueName.contains("dead-letter") || queueName.contains("dlq")) {
            return localDlq;
        } else {
            return localQueue;
        }
    }
}
