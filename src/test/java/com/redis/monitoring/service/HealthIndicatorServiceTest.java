package com.redis.monitoring.service;

import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.reliability.dto.ModuleHealthResponse;
import com.redis.monitoring.entity.DatabaseHealthIndicator;
import com.redis.monitoring.entity.RedisHealthIndicator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class HealthIndicatorServiceTest {

    @MockBean
    private JdbcTemplate mockJdbcTemplate;

    @MockBean
    private RedisConnectionFactory mockRedisConnectionFactory;

    @Autowired
    private DatabaseHealthIndicator databaseHealthIndicator;

    @Autowired
    private RedisHealthIndicator redisHealthIndicator;

    @Test
    void testDatabaseHealthIndicatorExceptionHandling() {
        // Mock DB connection throw exception
        when(mockJdbcTemplate.queryForObject(anyString(), eq(Integer.class)))
                .thenThrow(new RuntimeException("SQL Transient connection failure"));

        ModuleHealthResponse health = databaseHealthIndicator.checkHealth();
        assertEquals("Database", health.getModuleName());
        assertEquals("DOWN", health.getStatus()); // Handled cleanly as DOWN
    }

    @Test
    void testRedisHealthIndicatorExceptionHandling() {
        // Mock Redis connection factory throw exception
        when(mockRedisConnectionFactory.getConnection())
                .thenThrow(new RuntimeException("Redis connection refused"));

        ModuleHealthResponse health = redisHealthIndicator.checkHealth();
        assertEquals("Redis", health.getModuleName());
        assertEquals("DEGRADED", health.getStatus()); // Handled cleanly as DEGRADED
    }
}
