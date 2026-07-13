package com.redis.notification.service;

import com.redis.auth.repository.RefreshTokenRepository;
import com.redis.notification.repository.NotificationRepository;
import com.redis.user.repository.UserSessionRepository;
import com.redis.cart.repository.WishlistRepository;

import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.notification.dto.request.NotificationPreferenceRequest;
import com.redis.notification.dto.response.NotificationPreferenceResponse;
import com.redis.user.entity.User;
import com.redis.user.entity.Role;
import com.redis.notification.entity.NotificationChannel;
import com.redis.cart.repository.CartRepository;
import com.redis.user.repository.UserRepository;
import com.redis.notification.repository.UserNotificationPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class NotificationPreferenceServiceTest {

    @Autowired
    private UserNotificationPreferenceService preferenceService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private UserNotificationPreferenceRepository preferenceRepository;

    @Autowired
    private com.redis.notification.repository.NotificationRepository notificationRepository;

    @Autowired
    private com.redis.auth.repository.RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private com.redis.user.repository.UserSessionRepository userSessionRepository;

    @Autowired
    private com.redis.cart.repository.WishlistRepository wishlistRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        preferenceRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userSessionRepository.deleteAll();
        wishlistRepository.deleteAll();
        cartRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .username("prefuser")
                .email("pref@example.com")
                .password("Password123!")
                .role(Role.ROLE_USER)
                .accountEnabled(true)
                .accountNonLocked(true)
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    void testPreferenceLifecycle() {
        NotificationPreferenceResponse pref = preferenceService.getPreferences(testUser.getId());
        assertNotNull(pref.getId());
        assertTrue(pref.isEmailEnabled());
        assertTrue(pref.isSmsEnabled());

        NotificationPreferenceRequest update = NotificationPreferenceRequest.builder()
                .emailEnabled(false)
                .smsEnabled(true)
                .pushEnabled(false)
                .inAppEnabled(true)
                .marketingEnabled(false)
                .build();

        NotificationPreferenceResponse updated = preferenceService.updatePreferences(testUser.getId(), update);
        assertFalse(updated.isEmailEnabled());
        assertTrue(updated.isSmsEnabled());

        NotificationPreferenceResponse reset = preferenceService.resetDefaults(testUser.getId());
        assertTrue(reset.isEmailEnabled());
    }
}
