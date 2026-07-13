package com.redis.notification.service;

import com.redis.notification.dto.response.NotificationResponse;
import com.redis.notification.dto.response.NotificationSummaryResponse;
import com.redis.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    Page<NotificationResponse> getMyNotifications(User user, Pageable pageable);
    Page<NotificationResponse> getUnreadNotifications(User user, Pageable pageable);
    NotificationResponse getNotification(Long notificationId, User user);
    NotificationSummaryResponse getNotificationSummary(User user);
    NotificationResponse markAsRead(Long notificationId, User user);
    void markAllAsRead(User user);
    long getUnreadCount(User user);
    void deleteNotification(Long notificationId, User user);
}
