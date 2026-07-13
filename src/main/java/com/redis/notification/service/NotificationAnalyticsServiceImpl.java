package com.redis.notification.service;

import com.redis.notification.entity.NotificationChannel;
import com.redis.notification.dto.response.NotificationPriorityStatisticsResponse;
import com.redis.notification.dto.response.NotificationDashboardResponse;
import com.redis.notification.dto.response.NotificationTypeStatisticsResponse;
import com.redis.notification.entity.NotificationType;
import com.redis.notification.entity.NotificationStatus;
import com.redis.notification.entity.NotificationPriority;
import com.redis.notification.dto.response.NotificationAnalyticsResponse;
import com.redis.notification.dto.response.NotificationResponse;
import com.redis.notification.dto.response.NotificationChannelStatisticsResponse;

import com.redis.notification.entity.Notification;
import com.redis.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationAnalyticsServiceImpl implements NotificationAnalyticsService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public NotificationDashboardResponse getDashboard(LocalDateTime start, LocalDateTime end) {
        return NotificationDashboardResponse.builder()
                .overallSummary(getOverallAnalytics(start, end))
                .channelStatistics(getChannelStatistics(start, end))
                .typeStatistics(getTypeStatistics(start, end))
                .priorityStatistics(getPriorityStatistics(start, end))
                .retryStatistics(getRetryStatistics(start, end))
                .dlqStatistics(getDeadLetterAnalytics(start, end))
                .recentFailures(getRecentFailures(start, end, 0, 10))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationAnalyticsResponse getOverallAnalytics(LocalDateTime start, LocalDateTime end) {
        long total = getCount(start, end, null);
        long sent = getCount(start, end, NotificationStatus.SENT);
        long failed = getCount(start, end, NotificationStatus.FAILED);
        long retrying = getCount(start, end, NotificationStatus.RETRYING);
        long deadLetter = getCount(start, end, NotificationStatus.DEAD_LETTER);

        double deliverySuccessRate = total > 0 ? ((double) sent / total) * 100.0 : 0.0;

        long totalRetries = (start == null || end == null)
                ? notificationRepository.countByRetryCountGreaterThanZero()
                : notificationRepository.countByRetryCountGreaterThanZeroBetween(start, end);
        long successRetries = (start == null || end == null)
                ? notificationRepository.countByStatusAndRetryCountGreaterThanZero(NotificationStatus.SENT)
                : notificationRepository.countByStatusAndRetryCountGreaterThanZeroBetween(NotificationStatus.SENT, start, end);
        double retrySuccessRate = totalRetries > 0 ? ((double) successRetries / totalRetries) * 100.0 : 0.0;

        Double avgRetry = (start == null || end == null)
                ? notificationRepository.getAverageRetryCount()
                : notificationRepository.averageRetryCountBetween(start, end);
        double averageRetryCount = avgRetry != null ? avgRetry : 0.0;

        List<Object[]> deliveryTimes = (start == null || end == null)
                ? notificationRepository.getDeliveryTimestamps()
                : notificationRepository.getDeliveryTimestampsBetween(start, end);
        double averageDeliveryTimeMillis = 0.0;
        if (!deliveryTimes.isEmpty()) {
            double totalSeconds = 0.0;
            for (Object[] row : deliveryTimes) {
                LocalDateTime created = (LocalDateTime) row[0];
                LocalDateTime delivered = (LocalDateTime) row[1];
                totalSeconds += java.time.Duration.between(created, delivered).toMillis() / 1000.0;
            }
            averageDeliveryTimeMillis = (totalSeconds / deliveryTimes.size()) * 1000.0;
        }

        List<Object[]> resolutionTimes = (start == null || end == null)
                ? notificationRepository.getResolutionTimestamps()
                : notificationRepository.getResolutionTimestampsBetween(start, end);
        double averageResolutionTimeMillis = 0.0;
        if (!resolutionTimes.isEmpty()) {
            double totalSeconds = 0.0;
            for (Object[] row : resolutionTimes) {
                LocalDateTime created = (LocalDateTime) row[0];
                LocalDateTime resolved = (LocalDateTime) row[1];
                totalSeconds += java.time.Duration.between(created, resolved).toMillis() / 1000.0;
            }
            averageResolutionTimeMillis = (totalSeconds / resolutionTimes.size()) * 1000.0;
        }

        return NotificationAnalyticsResponse.builder()
                .totalNotifications(total)
                .totalSent(sent)
                .totalFailed(failed)
                .totalRetrying(retrying)
                .totalDeadLetters(deadLetter)
                .deliverySuccessRate(deliverySuccessRate)
                .retrySuccessRate(retrySuccessRate)
                .averageRetryCount(averageRetryCount)
                .averageDeliveryTimeMillis(averageDeliveryTimeMillis)
                .averageResolutionTimeMillis(averageResolutionTimeMillis)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationChannelStatisticsResponse> getChannelStatistics(LocalDateTime start, LocalDateTime end) {
        List<NotificationChannelStatisticsResponse> list = new ArrayList<>();
        for (NotificationChannel channel : NotificationChannel.values()) {
            long total = (start == null || end == null)
                    ? notificationRepository.countByChannel(channel)
                    : notificationRepository.countByChannelAndCreatedAtBetween(channel, start, end);

            List<Object[]> statusCounts = (start == null || end == null)
                    ? notificationRepository.countGroupedByStatusForChannel(channel)
                    : notificationRepository.countGroupedByStatusForChannelBetween(channel, start, end);

            long sent = 0, failed = 0, retrying = 0, deadLetter = 0;
            for (Object[] row : statusCounts) {
                NotificationStatus status = row[0] instanceof NotificationStatus ? (NotificationStatus) row[0] : NotificationStatus.valueOf(row[0].toString());
                long count = (Long) row[1];
                if (status == NotificationStatus.SENT) sent = count;
                else if (status == NotificationStatus.FAILED) failed = count;
                else if (status == NotificationStatus.RETRYING) retrying = count;
                else if (status == NotificationStatus.DEAD_LETTER) deadLetter = count;
            }

            double successRate = total > 0 ? ((double) sent / total) * 100.0 : 0.0;

            list.add(NotificationChannelStatisticsResponse.builder()
                    .channel(channel.name())
                    .total(total)
                    .sent(sent)
                    .failed(failed)
                    .retrying(retrying)
                    .deadLetter(deadLetter)
                    .successRate(successRate)
                    .build());
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationTypeStatisticsResponse> getTypeStatistics(LocalDateTime start, LocalDateTime end) {
        List<NotificationTypeStatisticsResponse> list = new ArrayList<>();
        for (NotificationType type : NotificationType.values()) {
            long total = (start == null || end == null)
                    ? notificationRepository.countTotalByType(type)
                    : notificationRepository.countTotalByTypeBetween(type, start, end);
            long sent = (start == null || end == null)
                    ? notificationRepository.countSentByType(type)
                    : notificationRepository.countSentByTypeBetween(type, start, end);
            double successRate = total > 0 ? ((double) sent / total) * 100.0 : 0.0;

            list.add(NotificationTypeStatisticsResponse.builder()
                    .notificationType(type.name())
                    .count(total)
                    .successRate(successRate)
                    .build());
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationPriorityStatisticsResponse> getPriorityStatistics(LocalDateTime start, LocalDateTime end) {
        List<Object[]> results = (start == null || end == null)
                ? notificationRepository.countGroupedByPriority()
                : notificationRepository.countGroupedByPriorityBetween(start, end);

        Map<String, Long> map = new HashMap<>();
        for (NotificationPriority priority : NotificationPriority.values()) {
            map.put(priority.name(), 0L);
        }
        for (Object[] row : results) {
            String key = row[0] instanceof NotificationPriority ? ((NotificationPriority) row[0]).name() : row[0].toString();
            map.put(key, (Long) row[1]);
        }

        return map.entrySet().stream()
                .map(e -> NotificationPriorityStatisticsResponse.builder()
                        .priority(e.getKey())
                        .count(e.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationAnalyticsResponse getRetryStatistics(LocalDateTime start, LocalDateTime end) {
        return getOverallAnalytics(start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getDeadLetterSummary(LocalDateTime start, LocalDateTime end, int page, int size) {
        List<Notification> list = (start == null || end == null)
                ? notificationRepository.findRecentDeadLetters(PageRequest.of(page, size))
                : notificationRepository.findRecentDeadLettersBetween(start, end, PageRequest.of(page, size));

        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getRecentFailures(LocalDateTime start, LocalDateTime end, int page, int size) {
        List<Notification> list = (start == null || end == null)
                ? notificationRepository.findRecentFailures(PageRequest.of(page, size))
                : notificationRepository.findRecentFailuresBetween(start, end, PageRequest.of(page, size));

        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    private NotificationAnalyticsResponse getDeadLetterAnalytics(LocalDateTime start, LocalDateTime end) {
        return getOverallAnalytics(start, end);
    }

    private long getCount(LocalDateTime start, LocalDateTime end, NotificationStatus status) {
        if (status == null) {
            return (start == null || end == null)
                    ? notificationRepository.count()
                    : notificationRepository.countByStatusAndCreatedAtBetween(NotificationStatus.SENT, start, end)
                    + notificationRepository.countByStatusAndCreatedAtBetween(NotificationStatus.FAILED, start, end)
                    + notificationRepository.countByStatusAndCreatedAtBetween(NotificationStatus.PENDING, start, end)
                    + notificationRepository.countByStatusAndCreatedAtBetween(NotificationStatus.DELIVERING, start, end)
                    + notificationRepository.countByStatusAndCreatedAtBetween(NotificationStatus.RETRYING, start, end)
                    + notificationRepository.countByStatusAndCreatedAtBetween(NotificationStatus.DEAD_LETTER, start, end);
        }
        return (start == null || end == null)
                ? notificationRepository.countByStatus(status)
                : notificationRepository.countByStatusAndCreatedAtBetween(status, start, end);
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUser().getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType().name())
                .channel(notification.getChannel().name())
                .priority(notification.getPriority().name())
                .status(notification.getStatus().name())
                .readStatus(notification.isReadStatus())
                .createdAt(notification.getCreatedAt())
                .deliveredAt(notification.getDeliveredAt())
                .readAt(notification.getReadAt())
                .resolvedAt(notification.getResolvedAt())
                .retryCount(notification.getRetryCount())
                .failureReason(notification.getFailureReason())
                .build();
    }
}
