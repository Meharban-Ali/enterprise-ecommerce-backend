package com.redis.webhook.dto.response;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookDashboardResponse {
    private List<WebhookStatisticsResponse> endpointsStats;
    private long webhookQueueSize;
    private long webhookRetryQueue;
    private long webhookDlq;
    private long circuitOpenCount;
    private long healthyEndpoints;
    private long disabledEndpoints;
    private double averageResponseTimeMs;
    private double averagePayloadSizeBytes;

    // Trends & Highlights
    private List<WebhookStatisticsResponse> topFailedEndpoints;
    private List<WebhookStatisticsResponse> slowestEndpoints;
    private List<WebhookResponse> mostActiveEndpoints;
    private Map<String, Long> retryTrend;
    private Map<String, Long> failureTrend;
    private Map<String, Long> successTrend;
}
