package com.redis.infrastructure.governance.dto;

import com.redis.security.dto.response.ApiKeyResponse;

import lombok.*;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiGovernanceDashboardResponse {

    private Long totalRequests;
    private Double successRate;
    private Map<String, Long> errorDistribution;
    private Double averageResponseTimeMs;
    private Double p95LatencyMs;
    private Double p99LatencyMs;
    
    private Map<String, Double> slowestApis;
    private Map<String, Long> mostCalledApis;
    
    private Long rateLimitViolations;
    private Long idempotentHits;
    private Long replayAttacksBlocked;
    private Long validationFailures;
    
    private Double averagePayloadSizeBytes;
    private Double requestsPerMinute;
    
    private Map<String, Long> topConsumers;
    private Map<String, Long> apiKeyUsage;

    private Long activeApiKeys;
    private Long lockedApiKeys;
    private Long rotatedKeys;
    private Double averageResponseSizeBytes;
    private Long maxResponseSizeBytes;
    private Map<String, Double> endpointLatencyRanking;
    private Map<String, ApiKeyResponse> consumerAnalytics;
}
