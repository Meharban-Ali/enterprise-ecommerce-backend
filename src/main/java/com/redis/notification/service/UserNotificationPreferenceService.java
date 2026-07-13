package com.redis.notification.service;

import com.redis.notification.dto.request.NotificationPreferenceRequest;
import com.redis.notification.dto.response.NotificationPreferenceResponse;
import com.redis.notification.entity.NotificationChannel;

public interface UserNotificationPreferenceService {

    NotificationPreferenceResponse getPreferences(Long userId);

    NotificationPreferenceResponse updatePreferences(Long userId, NotificationPreferenceRequest request);

    NotificationPreferenceResponse enableChannel(Long userId, NotificationChannel channel);

    NotificationPreferenceResponse disableChannel(Long userId, NotificationChannel channel);

    NotificationPreferenceResponse resetDefaults(Long userId);
}
