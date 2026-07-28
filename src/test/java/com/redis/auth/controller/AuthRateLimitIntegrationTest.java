package com.redis.auth.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@DisplayName("Auth Endpoint Rate Limiting Real HTTP Integration Test")
class AuthRateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        if (redisTemplate != null) {
            try {
                redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
            } catch (Exception ignored) {}
        }
    }

    @Test
    @DisplayName("✅ Enforces rate limit after 5 failed login attempts returning 429 Too Many Requests on 6th attempt")
    void testAuthLoginRateLimitingEnforcement() throws Exception {
        String testIp = "203.0.113." + (java.util.UUID.randomUUID().hashCode() & 0x7FFFFFFF) % 240;
        String jsonPayload = "{\"email\":\"invalid.user@example.com\",\"password\":\"WrongPass123!\"}";

        // Requests 1 through 5: Should pass through filter (return 401 Bad Credentials, NOT 429)
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonPayload)
                            .header("X-Forwarded-For", testIp))
                    .andExpect(status().isUnauthorized());
        }

        // Request 6: Must be intercepted by RateLimitingFilter and return 429 Too Many Requests
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload)
                        .header("X-Forwarded-For", testIp))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }
}
