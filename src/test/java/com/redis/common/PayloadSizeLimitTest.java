package com.redis.common;

import com.redis.infrastructure.security.RequestLoggingFilter;

import com.redis.infrastructure.config.ApiSecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;

public class PayloadSizeLimitTest {

    @Test
    void testOversizedPayloadBlocked() throws Exception {
        ApiSecurityProperties props = new ApiSecurityProperties();
        props.setPayloadProtectionEnabled(true);
        props.setMaxRequestBodySize(10);

        RequestLoggingFilter filter = new RequestLoggingFilter(props, null, null);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/products");
        request.setContent("This body is longer than ten bytes".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(413, response.getStatus());
        assertTrue(response.getContentAsString().contains("Payload Too Large"));
    }
}
