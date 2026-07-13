package com.redis.security.entity;

import com.redis.infrastructure.security.ApiKeyAuthenticationFilter;

import com.redis.infrastructure.config.ApiSecurityProperties;
import com.redis.security.entity.ApiKey;
import com.redis.security.repository.ApiKeyRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;

public class ApiKeyLockoutTest {

    @Test
    void testApiKeyLockoutFailsFast() throws Exception {
        ApiKeyRepository mockRepo = Mockito.mock(ApiKeyRepository.class);
        ApiKey lockedKey = ApiKey.builder()
                .name("test-key")
                .keyHash("somehash")
                .enabled(true)
                .revoked(false)
                .lockUntil(LocalDateTime.now().plusMinutes(10))
                .build();
        Mockito.when(mockRepo.findByKeyHashOrRotationKeyHash(anyString())).thenReturn(Optional.of(lockedKey));

        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(mockRepo, null, new ApiSecurityProperties());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-API-Key", "testkey123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("API Key is locked out"));
    }
}
