package com.redis.notification.entity;

import com.redis.notification.service.NotificationChannelService;

import com.redis.notification.entity.NotificationChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationChannelFactoryTest {

    @Mock
    private NotificationChannelService emailService;

    @Mock
    private NotificationChannelService smsService;

    private NotificationChannelFactory factory;

    @BeforeEach
    void setUp() {
        // Stub using thenAnswer to handle all input arguments leniently and avoid PotentialStubbingProblem
        when(emailService.supports(any(NotificationChannel.class))).thenAnswer(invocation -> {
            NotificationChannel channel = invocation.getArgument(0);
            return channel == NotificationChannel.EMAIL;
        });

        when(smsService.supports(any(NotificationChannel.class))).thenAnswer(invocation -> {
            NotificationChannel channel = invocation.getArgument(0);
            return channel == NotificationChannel.SMS;
        });

        factory = new NotificationChannelFactory(List.of(emailService, smsService));
    }

    @Test
    void testGetEmailServiceSuccess() {
        NotificationChannelService service = factory.getService(NotificationChannel.EMAIL);
        assertNotNull(service);
        assertEquals(emailService, service);
    }

    @Test
    void testGetSmsServiceSuccess() {
        NotificationChannelService service = factory.getService(NotificationChannel.SMS);
        assertNotNull(service);
        assertEquals(smsService, service);
    }

    @Test
    void testGetUnsupportedServiceThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> factory.getService(NotificationChannel.WEBSOCKET));
    }
}
