package com.redis.notification.event;

import com.redis.order.entity.Order;
import com.redis.notification.service.NotificationOutboxService;

import com.redis.infrastructure.config.NotificationProperties;
import com.redis.order.event.OrderNotificationEvent;
import com.redis.notification.entity.NotificationChannel;
import com.redis.notification.entity.NotificationPriority;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationEventPublisherTest {

    @Mock
    private NotificationProperties properties;

    @Mock
    private NotificationOutboxService outboxService;

    @Mock
    private NotificationEventBusFactory eventBusFactory;

    @Mock
    private NotificationEventBus eventBus;

    @InjectMocks
    private NotificationEventPublisher notificationEventPublisher;

    @Test
    void testPublishEventDelegatesCorrectly() {
        when(properties.isOutboxEnabled()).thenReturn(false);
        when(eventBusFactory.getEventBus()).thenReturn(eventBus);

        OrderNotificationEvent event = new OrderNotificationEvent(
                this,
                1L,
                "Order Created",
                "Your order #1 has been processed.",
                NotificationChannel.EMAIL,
                NotificationPriority.HIGH
        );

        notificationEventPublisher.publishEvent(event);

        verify(eventBus, times(1)).publish(event);
    }
}
