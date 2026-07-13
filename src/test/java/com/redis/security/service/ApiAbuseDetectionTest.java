package com.redis.security.service;

import com.redis.notification.event.NotificationEventPublisher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ApiAbuseDetectionTest {

    @Mock
    private NotificationEventPublisher eventPublisher;

    @InjectMocks
    private ApiAbuseDetectionServiceImpl abuseDetectionService;

    @Test
    void testAbuseDetectionTriggersAlert() {
        String clientIp = "10.0.0.1";
        
        for (int i = 0; i < 10; i++) {
            abuseDetectionService.recordViolation(clientIp, "SUSPICIOUS_PROBING");
        }

        verify(eventPublisher, times(1)).publishCriticalAlert(
                contains("SECURITY ALERT: Suspicious API Abuse"),
                contains("10.0.0.1")
        );
    }
}
