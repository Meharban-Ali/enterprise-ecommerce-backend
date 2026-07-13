package com.redis.notification.event;

import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.notification.event.AuthenticationNotificationEvent;
import com.redis.notification.entity.NotificationChannel;
import com.redis.notification.entity.NotificationPriority;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class KafkaNotificationEventBusTest {

    @Autowired
    private KafkaNotificationEventBus kafkaEventBus;

    @Test
    void testKafkaMockPublishing() {
        AuthenticationNotificationEvent event = new AuthenticationNotificationEvent(
                this, 456L, "Kafka Title", "Kafka Message", NotificationChannel.EMAIL, NotificationPriority.MEDIUM
        );
        
        assertDoesNotThrow(() -> kafkaEventBus.publish(event));
    }
}
