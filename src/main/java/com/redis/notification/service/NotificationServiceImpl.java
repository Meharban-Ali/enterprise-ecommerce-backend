package com.redis.notification.service;

import com.redis.notification.dto.response.NotificationResponse;
import com.redis.notification.dto.response.NotificationSummaryResponse;
import com.redis.notification.entity.Notification;
import com.redis.user.entity.User;
import com.redis.notification.entity.NotificationStatus;
import com.redis.notification.mapper.NotificationMapper;
import com.redis.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(User user, Pageable pageable) {
        log.info("Fetching notifications for user ID: {}", user.getId());
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(notificationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUnreadNotifications(User user, Pageable pageable) {
        log.info("Fetching unread notifications for user ID: {}", user.getId());
        return notificationRepository.findByUserIdAndReadStatus(user.getId(), false, pageable)
                .map(notificationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getNotification(Long notificationId, User user) {
        log.info("Fetching notification ID: {} for user ID: {}", notificationId, user.getId());
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with ID: " + notificationId));

        validateOwnership(notification, user);

        return notificationMapper.toResponse(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationSummaryResponse getNotificationSummary(User user) {
        log.info("Generating notification summary for user ID: {}", user.getId());
        long unreadCount = notificationRepository.countByUserIdAndReadStatus(user.getId(), false);
        List<Notification> recent = notificationRepository.findTop20ByUserIdOrderByCreatedAtDesc(user.getId());

        List<NotificationResponse> responses = recent.stream()
                .map(notificationMapper::toResponse)
                .collect(Collectors.toList());

        LocalDateTime lastNotificationTime = recent.isEmpty() ? null : recent.get(0).getCreatedAt();

        return NotificationSummaryResponse.builder()
                .unreadCount(unreadCount)
                .recentNotifications(responses)
                .lastNotificationTime(lastNotificationTime)
                .build();
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long notificationId, User user) {
        log.info("Marking notification ID: {} as read for user ID: {}", notificationId, user.getId());
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with ID: " + notificationId));

        validateOwnership(notification, user);

        if (!notification.isReadStatus()) {
            notification.setReadStatus(true);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
        }

        return notificationMapper.toResponse(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(User user) {
        log.info("Marking all notifications as read for user ID: {}", user.getId());
        List<Notification> unreadList = notificationRepository.findTop20ByUserIdOrderByCreatedAtDesc(user.getId());
        // Since we want to mark all, we can iterate or load all unread
        // For simplicity and safety, we find and mark unread items
        List<Notification> unread = notificationRepository.findAll().stream()
                .filter(n -> n.getUser().getId().equals(user.getId()) && !n.isReadStatus())
                .collect(Collectors.toList());

        for (Notification n : unread) {
            n.setReadStatus(true);
            n.setReadAt(LocalDateTime.now());
        }
        notificationRepository.saveAll(unread);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        return notificationRepository.countByUserIdAndReadStatus(user.getId(), false);
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId, User user) {
        log.info("Deleting notification ID: {} for user ID: {}", notificationId, user.getId());
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with ID: " + notificationId));

        validateOwnership(notification, user);

        notificationRepository.delete(notification);
    }

    private void validateOwnership(Notification notification, User user) {
        if (!notification.getUser().getId().equals(user.getId())) {
            log.warn("BOLA attempt detected! User ID: {} tried to access notification belonging to User ID: {}",
                    user.getId(), notification.getUser().getId());
            throw new AccessDeniedException("Access denied: You do not own this notification.");
        }
    }
}
