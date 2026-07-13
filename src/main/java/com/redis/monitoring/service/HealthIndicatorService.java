package com.redis.monitoring.service;

import com.redis.reliability.dto.ModuleHealthResponse;

public interface HealthIndicatorService {
    String getName();
    ModuleHealthResponse checkHealth();
}
