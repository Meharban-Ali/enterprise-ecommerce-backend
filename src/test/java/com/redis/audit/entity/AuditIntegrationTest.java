package com.redis.audit.entity;

import com.redis.notification.repository.UserNotificationPreferenceRepository;
import com.redis.user.service.UserService;
import com.redis.auth.repository.RefreshTokenRepository;
import com.redis.user.repository.UserSessionRepository;
import com.redis.notification.repository.NotificationRepository;
import com.redis.cart.repository.WishlistRepository;

import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.auth.dto.request.RegisterRequest;
import com.redis.audit.entity.AuditLog;
import com.redis.audit.entity.AuditActionType;
import com.redis.audit.entity.AuditStatus;
import com.redis.audit.repository.AuditLogRepository;
import com.redis.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class AuditIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private com.redis.auth.repository.RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private com.redis.user.repository.UserSessionRepository userSessionRepository;

    @Autowired
    private com.redis.cart.repository.WishlistRepository wishlistRepository;

    @Autowired
    private com.redis.notification.repository.NotificationRepository notificationRepository;

    @Autowired
    private com.redis.notification.repository.UserNotificationPreferenceRepository preferenceRepository;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        notificationRepository.deleteAll();
        preferenceRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userSessionRepository.deleteAll();
        wishlistRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testRegisterUserTriggersAuditLog() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("audited_user")
                .email("audit@example.com")
                .password("Password123!")
                .securityQuestion("Quest")
                .securityAnswer("Answ")
                .build();

        userService.registerUser(request);

        // Sleep to let async event processing pool complete
        Thread.sleep(1000);

        List<AuditLog> logs = auditLogRepository.findAll();
        assertTrue(logs.size() >= 1);
        boolean hasRegisterEvent = logs.stream().anyMatch(l -> l.getActionType() == AuditActionType.REGISTER);
        assertTrue(hasRegisterEvent);
    }
}
