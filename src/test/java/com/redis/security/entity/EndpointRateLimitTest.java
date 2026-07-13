package com.redis.security.entity;

import com.redis.infrastructure.security.RateLimitingFilter;

import com.redis.infrastructure.config.ApiSecurityProperties;
import com.redis.security.service.RateLimitService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

public class EndpointRateLimitTest {

    @Test
    void testEndpointRateLimitOverride() throws Exception {
        SecurityContextHolder.clearContext();

        RateLimitService mockRateLimitService = Mockito.mock(RateLimitService.class);
        Mockito.when(mockRateLimitService.isAllowed(anyString(), anyInt(), anyInt())).thenReturn(false);
        Mockito.when(mockRateLimitService.getRetryAfterSeconds(anyString(), anyInt(), anyInt())).thenReturn(15);

        ApiSecurityProperties props = new ApiSecurityProperties();
        props.setRateLimitEnabled(true);
        props.setEndpointRateLimitEnabled(true);
        Map<String, Integer> overrides = new HashMap<>();
        overrides.put("POST /api/orders", 5);
        props.setEndpointRateLimits(overrides);

        RateLimitingFilter filter = new RateLimitingFilter(mockRateLimitService, props, null, null);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(429, response.getStatus());
        assertEquals("15", response.getHeader("Retry-After"));
        assertTrue(Boolean.TRUE.equals(request.getAttribute("rateLimitExceeded")));
        assertEquals("ip:127.0.0.1:POST:/api/orders", request.getAttribute("consumerKey"));
    }
}
