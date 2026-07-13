package com.redis.common;

import com.redis.infrastructure.security.MultiReadHttpServletRequestWrapper;

import com.redis.infrastructure.config.ApiSecurityProperties;
import com.redis.security.entity.IdempotencyInterceptor;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RequestFingerprintTest {

    @Test
    void testRequestFingerprintGeneration() throws IOException {
        IdempotencyInterceptor interceptor = new IdempotencyInterceptor(new com.redis.infrastructure.config.ApiSecurityProperties());

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/orders");
        request.setQueryString("param=value");
        request.setContent("{\"orderId\":1}".getBytes(StandardCharsets.UTF_8));
        request.setRemoteAddr("192.168.1.1");

        MultiReadHttpServletRequestWrapper wrapper = new MultiReadHttpServletRequestWrapper(request);
        String f1 = interceptor.generateFingerprint(wrapper);

        // Same request details must produce same fingerprint
        MockHttpServletRequest request2 = new MockHttpServletRequest("POST", "/api/v1/orders");
        request2.setQueryString("param=value");
        request2.setContent("{\"orderId\":1}".getBytes(StandardCharsets.UTF_8));
        request2.setRemoteAddr("192.168.1.1");

        MultiReadHttpServletRequestWrapper wrapper2 = new MultiReadHttpServletRequestWrapper(request2);
        String f2 = interceptor.generateFingerprint(wrapper2);

        assertEquals(f1, f2);

        // Modifying IP should change fingerprint
        request2.setRemoteAddr("192.168.1.2");
        MultiReadHttpServletRequestWrapper wrapper3 = new MultiReadHttpServletRequestWrapper(request2);
        String f3 = interceptor.generateFingerprint(wrapper3);

        assertNotEquals(f1, f3);
    }
}