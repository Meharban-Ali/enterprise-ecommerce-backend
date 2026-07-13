package com.redis.notification.service;

import com.redis.notification.entity.NotificationChannel;
import com.redis.notification.entity.NotificationStatus;
import com.redis.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationMetricsServiceImpl implements NotificationMetricsService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public long totalSent() {
        return notificationRepository.countByStatus(NotificationStatus.SENT);
    }

    @Override
    @Transactional(readOnly = true)
    public long totalFailed() {
        return notificationRepository.countByStatus(NotificationStatus.FAILED);
    }

    @Override
    @Transactional(readOnly = true)
    public long totalDeadLetters() {
        return notificationRepository.countByStatus(NotificationStatus.DEAD_LETTER);
    }

    @Override
    @Transactional(readOnly = true)
    public double retrySuccessRate() {
        long totalRetries = notificationRepository.countByRetryCountGreaterThanZero();
        if (totalRetries == 0) {
            return 0.0;
        }
        long successRetries = notificationRepository.countByStatusAndRetryCountGreaterThanZero(NotificationStatus.SENT);
        return ((double) successRetries / totalRetries) * 100.0;
    }

    @Override
    @Transactional(readOnly = true)
    public double averageRetryCount() {
        Double avg = notificationRepository.averageRetryCount();
        return avg != null ? avg : 0.0;
    }

    @Override
    @Transactional(readOnly = true)
    public double averageDeliveryTime() {
        List<Object[]> rows = notificationRepository.getDeliveryTimestamps(PageRequest.of(0, 1000));
        if (rows.isEmpty()) {
            return 0.0;
        }
        long totalSeconds = 0;
        for (Object[] row : rows) {
            LocalDateTime created = (LocalDateTime) row[0];
            LocalDateTime delivered = (LocalDateTime) row[1];
            totalSeconds += Duration.between(created, delivered).toSeconds();
        }
        return (double) totalSeconds / rows.size();
    }

    @Override
    @Transactional(readOnly = true)
    public double averageRetryResolutionTime() {
        List<Object[]> rows = notificationRepository.getResolutionTimestamps(PageRequest.of(0, 1000));
        if (rows.isEmpty()) {
            return 0.0;
        }
        long totalSeconds = 0;
        for (Object[] row : rows) {
            LocalDateTime created = (LocalDateTime) row[0];
            LocalDateTime resolved = (LocalDateTime) row[1];
            totalSeconds += Duration.between(created, resolved).toSeconds();
        }
        return (double) totalSeconds / rows.size();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<NotificationChannel, Long> channelStatistics() {
        Map<NotificationChannel, Long> stats = new EnumMap<>(NotificationChannel.class);
        
        // Initialize all channels with 0
        for (NotificationChannel channel : NotificationChannel.values()) {
            stats.put(channel, 0L);
        }

        List<Object[]> results = notificationRepository.countGroupedByChannel();
        for (Object[] row : results) {
            if (row[0] instanceof NotificationChannel) {
                stats.put((NotificationChannel) row[0], (Long) row[1]);
            } else if (row[0] instanceof String) {
                stats.put(NotificationChannel.valueOf((String) row[0]), (Long) row[1]);
            }
        }
        return stats;
    }
}
