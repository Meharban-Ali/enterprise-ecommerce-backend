package com.redis.security.entity;

import com.redis.infrastructure.security.RateLimitingFilter;

import com.redis.infrastructure.config.ApiSecurityProperties;
import com.redis.security.service.RateLimitService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

public class AdaptiveRateLimitTest {

    @Test
    void testAdaptiveRateLimitForAdmin() throws Exception {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "admin", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        RateLimitService mockRateLimitService = Mockito.mock(RateLimitService.class);
        ApiSecurityProperties props = new ApiSecurityProperties();
        props.setRateLimitEnabled(true);
        props.setAdaptiveRateLimitEnabled(true);

        RateLimitingFilter filter = new RateLimitingFilter(mockRateLimitService, props, null, null);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        Mockito.verifyNoInteractions(mockRateLimitService);
    }
}
