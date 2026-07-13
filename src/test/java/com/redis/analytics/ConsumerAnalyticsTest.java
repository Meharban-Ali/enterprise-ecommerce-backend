package com.redis.analytics;

import com.redis.infrastructure.config.ApiSecurityProperties;
import com.redis.security.entity.ApiKey;
import com.redis.security.repository.ApiKeyRepository;
import com.redis.security.service.ApiKeyServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

public class ConsumerAnalyticsTest {

    @Test
    void testConsumerUsageMetricsSaved() {
        ApiKeyRepository mockRepo = Mockito.mock(ApiKeyRepository.class);
        ApiKey key = ApiKey.builder()
                .id(1L)
                .name("test-integration")
                .keyHash("hash")
                .build();
        Mockito.when(mockRepo.findById(1L)).thenReturn(Optional.of(key));

        ApiSecurityProperties props = new ApiSecurityProperties();
        props.setConsumerAnalyticsEnabled(true);

        ApiKeyServiceImpl service = new ApiKeyServiceImpl(mockRepo, props);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        try {
            service.recordUsage(1L, 100L, true, "127.0.0.1", false);

            assertEquals(1, key.getTotalRequests());
            assertEquals(1L, key.getRequestsPerHour());
            assertEquals(1L, key.getRequestsPerDay());
            assertEquals(100.0, key.getSuccessRate());
            assertEquals(0.0, key.getErrorRate());
            assertTrue(key.getTopEndpointsJson().contains("GET /api/products:1"));
            Mockito.verify(mockRepo).save(any(ApiKey.class));
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }
}
