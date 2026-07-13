package com.redis.notification.event;

import com.redis.infrastructure.events.LocalEventBus;

import com.redis.infrastructure.config.NotificationProperties;
import com.redis.infrastructure.config.TestRedisConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class NotificationEventBusFactoryTest {

    @Autowired
    private NotificationEventBusFactory factory;

    @Autowired
    private NotificationProperties properties;

    @Test
    void testResolveBuses() {
        String oldType = properties.getEventBusType();

        properties.setEventBusType("LOCAL");
        NotificationEventBus bus1 = factory.getEventBus();
        assertTrue(bus1 instanceof LocalEventBus);

        properties.setEventBusType("KAFKA");
        NotificationEventBus bus2 = factory.getEventBus();
        assertTrue(bus2 instanceof KafkaNotificationEventBus);

        properties.setEventBusType(oldType);
    }
}
