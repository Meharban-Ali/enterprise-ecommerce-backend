package com.redis.reliability.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class ModuleHealthResponse {
    private String moduleName;
    private String status; // UP, WARNING, DEGRADED, DOWN
    @Builder.Default
    private String moduleVersion = "1.0.0";
    private String message;
    private Map<String, Object> details;
}
