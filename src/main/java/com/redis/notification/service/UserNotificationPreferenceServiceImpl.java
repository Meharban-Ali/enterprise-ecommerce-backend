package com.redis.notification.service;

import com.redis.infrastructure.config.NotificationProperties;
import com.redis.notification.dto.request.NotificationPreferenceRequest;
import com.redis.notification.dto.response.NotificationPreferenceResponse;
import com.redis.user.entity.User;
import com.redis.notification.entity.UserNotificationPreference;
import com.redis.notification.entity.NotificationChannel;
import com.redis.user.repository.UserRepository;
import com.redis.notification.repository.UserNotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserNotificationPreferenceServiceImpl implements UserNotificationPreferenceService {

    private final UserNotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;
    private final NotificationProperties properties;
    private final CacheManager cacheManager;

    @Override
    @Transactional
    public NotificationPreferenceResponse getPreferences(Long userId) {
        log.info("Fetching user preferences for user ID: {}", userId);
        UserNotificationPreference preference = getOrCreateDefaultPreferences(userId);
        return mapToResponse(preference);
    }

    @Override
    @Transactional
    public NotificationPreferenceResponse updatePreferences(Long userId, NotificationPreferenceRequest request) {
        log.info("Updating user preferences for user ID: {}", userId);
        UserNotificationPreference preference = getOrCreateDefaultPreferences(userId);

        preference.setEmailEnabled(request.isEmailEnabled());
        preference.setSmsEnabled(request.isSmsEnabled());
        preference.setPushEnabled(request.isPushEnabled());
        preference.setInAppEnabled(request.isInAppEnabled());
        preference.setMarketingEnabled(request.isMarketingEnabled());

        preference = preferenceRepository.save(preference);
        evictCache(userId);
        return mapToResponse(preference);
    }

    @Override
    @Transactional
    public NotificationPreferenceResponse enableChannel(Long userId, NotificationChannel channel) {
        log.info("Enabling channel: {} for user ID: {}", channel, userId);
        UserNotificationPreference preference = getOrCreateDefaultPreferences(userId);
        setChannelEnabled(preference, channel, true);
        preference = preferenceRepository.save(preference);
        evictCache(userId);
        return mapToResponse(preference);
    }

    @Override
    @Transactional
    public NotificationPreferenceResponse disableChannel(Long userId, NotificationChannel channel) {
        log.info("Disabling channel: {} for user ID: {}", channel, userId);
        UserNotificationPreference preference = getOrCreateDefaultPreferences(userId);
        setChannelEnabled(preference, channel, false);
        preference = preferenceRepository.save(preference);
        evictCache(userId);
        return mapToResponse(preference);
    }

    @Override
    @Transactional
    public NotificationPreferenceResponse resetDefaults(Long userId) {
        log.info("Resetting defaults for user ID: {}", userId);
        UserNotificationPreference preference = getOrCreateDefaultPreferences(userId);

        preference.setEmailEnabled(true);
        preference.setSmsEnabled(true);
        preference.setPushEnabled(true);
        preference.setInAppEnabled(true);
        preference.setMarketingEnabled(true);

        preference = preferenceRepository.save(preference);
        evictCache(userId);
        return mapToResponse(preference);
    }

    private UserNotificationPreference getOrCreateDefaultPreferences(Long userId) {
        return preferenceRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

            UserNotificationPreference defaultPref = UserNotificationPreference.builder()
                    .user(user)
                    .emailEnabled(true)
                    .smsEnabled(true)
                    .pushEnabled(true)
                    .inAppEnabled(true)
                    .marketingEnabled(true)
                    .securityMandatory(true)
                    .build();
            return preferenceRepository.save(defaultPref);
        });
    }

    private void setChannelEnabled(UserNotificationPreference preference, NotificationChannel channel, boolean enabled) {
        switch (channel) {
            case EMAIL -> preference.setEmailEnabled(enabled);
            case SMS -> preference.setSmsEnabled(enabled);
            case PUSH -> preference.setPushEnabled(enabled);
            case IN_APP -> preference.setInAppEnabled(enabled);
            default -> throw new IllegalArgumentException("Unsupported channel: " + channel);
        }
    }

    private void evictCache(Long userId) {
        if (properties.isTemplateCacheEnabled() && cacheManager != null) {
            Cache cache = cacheManager.getCache("user_notification_preferences");
            if (cache != null) {
                cache.evict(userId);
            }
        }
    }

    private NotificationPreferenceResponse mapToResponse(UserNotificationPreference entity) {
        return NotificationPreferenceResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .emailEnabled(entity.isEmailEnabled())
                .smsEnabled(entity.isSmsEnabled())
                .pushEnabled(entity.isPushEnabled())
                .inAppEnabled(entity.isInAppEnabled())
                .marketingEnabled(entity.isMarketingEnabled())
                .securityMandatory(entity.isSecurityMandatory())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
