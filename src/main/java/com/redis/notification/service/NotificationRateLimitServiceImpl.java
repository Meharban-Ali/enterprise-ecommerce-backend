package com.redis.notification.service;

import com.redis.infrastructure.config.NotificationProperties;
import com.redis.notification.entity.NotificationRateLimit;
import com.redis.notification.entity.NotificationChannel;
import com.redis.notification.entity.NotificationType;
import com.redis.notification.repository.NotificationRateLimitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationRateLimitServiceImpl implements NotificationRateLimitService {

    private final NotificationRateLimitRepository rateLimitRepository;
    private final NotificationProperties properties;

    @Override
    @Transactional
    public boolean shouldSend(Long userId, NotificationType type, NotificationChannel channel) {
        if (!properties.isRateLimitingEnabled()) {
            return true;
        }

        boolean hourlyOk = checkAndIncrement(userId, type, channel, "HOURLY", properties.getDefaultRateLimitPerHour(), 1);
        if (!hourlyOk) {
            log.warn("Rate limit exceeded (HOURLY) for userId: {}, type: {}, channel: {}", userId, type, channel);
            return false;
        }

        boolean dailyOk = checkAndIncrement(userId, type, channel, "DAILY", properties.getDefaultRateLimitPerDay(), 24);
        if (!dailyOk) {
            log.warn("Rate limit exceeded (DAILY) for userId: {}, type: {}, channel: {}", userId, type, channel);
            return false;
        }

        return true;
    }

    private boolean checkAndIncrement(Long userId, NotificationType type, NotificationChannel channel, 
                                      String windowType, int maxLimit, int hoursWindow) {
        LocalDateTime now = LocalDateTime.now();
        Optional<NotificationRateLimit> limitOpt = rateLimitRepository
                .findByUserIdAndNotificationTypeAndChannelAndWindowType(userId, type, channel, windowType);

        if (limitOpt.isEmpty()) {
            NotificationRateLimit limit = NotificationRateLimit.builder()
                    .userId(userId)
                    .notificationType(type)
                    .channel(channel)
                    .windowType(windowType)
                    .counter(1)
                    .windowStart(now)
                    .build();
            rateLimitRepository.save(limit);
            return true;
        }

        NotificationRateLimit limit = limitOpt.get();
        if (limit.getWindowStart().plusHours(hoursWindow).isBefore(now)) {
            limit.setCounter(1);
            limit.setWindowStart(now);
            rateLimitRepository.save(limit);
            return true;
        }

        if (limit.getCounter() >= maxLimit) {
            return false;
        }

        limit.setCounter(limit.getCounter() + 1);
        rateLimitRepository.save(limit);
        return true;
    }

    @Override
    @Transactional
    public void incrementCounter(Long userId, NotificationType type, NotificationChannel channel) {
        // Increment counter handled dynamically within shouldSend/checkAndIncrement structure.
    }

    @Override
    @Transactional(readOnly = true)
    public long calculateRemainingWindow(Long userId, NotificationType type, NotificationChannel channel, String windowType) {
        LocalDateTime now = LocalDateTime.now();
        return rateLimitRepository.findByUserIdAndNotificationTypeAndChannelAndWindowType(userId, type, channel, windowType)
                .map(limit -> {
                    int hours = "HOURLY".equalsIgnoreCase(windowType) ? 1 : 24;
                    LocalDateTime expiry = limit.getWindowStart().plusHours(hours);
                    if (expiry.isBefore(now)) {
                        return 0L;
                    }
                    return java.time.Duration.between(now, expiry).toSeconds();
                })
                .orElse(0L);
    }

    @Override
    @Transactional
    public void resetLimits(Long userId) {
        rateLimitRepository.deleteByUserId(userId);
        log.info("Reset all rate limits for userId: {}", userId);
    }
}
