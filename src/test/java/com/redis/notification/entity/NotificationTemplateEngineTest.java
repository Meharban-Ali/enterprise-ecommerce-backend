package com.redis.notification.entity;

import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.common.dto.WelcomeTemplateData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class NotificationTemplateEngineTest {

    @Autowired
    private TemplateEngine templateEngine;

    @Test
    void testWelcomeTemplateRendering() {
        WelcomeTemplateData data = WelcomeTemplateData.builder()
                .customerName("Alice")
                .companyName("Test Shop")
                .supportContact("support@testshop.com")
                .build();

        Context context = new Context();
        context.setVariable("data", data);

        String result = templateEngine.process("welcome", context);

        assertNotNull(result);
        assertTrue(result.contains("Welcome,"));
        assertTrue(result.contains("Alice"));
        assertTrue(result.contains("Test Shop"));
        assertTrue(result.contains("support@testshop.com"));
    }
}
