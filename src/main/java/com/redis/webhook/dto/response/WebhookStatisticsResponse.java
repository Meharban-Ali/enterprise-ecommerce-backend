package com.redis.webhook.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookStatisticsResponse {
    private Long endpointId;
    private String name;
    private String targetUrl;
    private double successPercentage;
    private double failurePercentage;
    private double retryPercentage;
    private double timeoutPercentage;
    private double averageLatencyMs;
    private long totalDeliveries;
    private long successCount;
    private long failureCount;
    private long retryCount;
    private long deadLetterCount;
    private double healthScore;
    private LocalDateTime lastSuccess;
    private LocalDateTime lastFailure;
}
