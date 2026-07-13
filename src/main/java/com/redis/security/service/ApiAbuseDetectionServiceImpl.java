package com.redis.security.service;

import com.redis.notification.event.NotificationEventPublisher;
import com.redis.incident.entity.Incident;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
public class ApiAbuseDetectionServiceImpl implements ApiAbuseDetectionService {

    @Autowired(required = false)
    private NotificationEventPublisher eventPublisher;

    private final Map<String, Queue<Long>> ipViolations = new ConcurrentHashMap<>();
    private final Map<String, Long> lastAlertTime = new ConcurrentHashMap<>();

    private static final int VIOLATION_THRESHOLD = 10;
    private static final int WINDOW_SECONDS = 300; // 5 minutes
    private static final long ALERT_COOLDOWN_MS = 60000; // 1 minute cooldown per IP

    @Override
    public void recordViolation(String clientIp, String violationType) {
        long now = System.currentTimeMillis();
        long windowStart = now - (WINDOW_SECONDS * 1000L);

        Queue<Long> queue = ipViolations.computeIfAbsent(clientIp, k -> new ConcurrentLinkedQueue<>());
        synchronized (queue) {
            queue.add(now);
            
            // Prune expired
            while (!queue.isEmpty() && queue.peek() < windowStart) {
                queue.poll();
            }

            int count = queue.size();
            log.warn("Security violation recorded for client IP: {} | Type: {} | Total window count: {}", clientIp, violationType, count);

            if (count >= VIOLATION_THRESHOLD) {
                triggerAlert(clientIp, violationType);
            }
        }
    }

    private void triggerAlert(String clientIp, String violationType) {
        long now = System.currentTimeMillis();
        Long lastAlert = lastAlertTime.get(clientIp);

        if (lastAlert == null || (now - lastAlert) > ALERT_COOLDOWN_MS) {
            lastAlertTime.put(clientIp, now);
            
            String title = "SECURITY ALERT: Suspicious API Abuse from " + clientIp;
            String message = String.format(
                    "Client IP %s generated %d security violations (Type: %s) within the last %d seconds. Possible probing or abuse attempt.",
                    clientIp, VIOLATION_THRESHOLD, violationType, WINDOW_SECONDS
            );

            log.error("API ABUSE DETECTED - Raising Security Incident: {} | Details: {}", title, message);

            if (eventPublisher != null) {
                try {
                    eventPublisher.publishCriticalAlert(title, message);
                } catch (Exception e) {
                    log.error("Failed to publish security alert incident", e);
                }
            }
        }
    }
}
