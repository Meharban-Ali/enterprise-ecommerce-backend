package com.redis.webhook.service;

import com.redis.webhook.repository.WebhookEndpointRepository;
import com.redis.webhook.dto.response.WebhookResponse;
import com.redis.webhook.entity.WebhookDelivery;
import com.redis.webhook.dto.response.WebhookDashboardResponse;
import com.redis.common.entity.CircuitState;
import com.redis.webhook.repository.WebhookDeliveryRepository;
import com.redis.webhook.dto.response.WebhookStatisticsResponse;
import com.redis.webhook.entity.WebhookStatus;
import com.redis.webhook.entity.WebhookEndpoint;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookMetricsServiceImpl implements WebhookMetricsService {

    private final WebhookEndpointRepository endpointRepository;
    private final WebhookDeliveryRepository deliveryRepository;

    @Override
    @Transactional(readOnly = true)
    public WebhookDashboardResponse getDashboard() {
        List<WebhookEndpoint> endpoints = endpointRepository.findAll();
        List<WebhookStatisticsResponse> statsList = endpoints.stream()
                .map(e -> getEndpointStats(e.getId()))
                .collect(Collectors.toList());

        long activeCount = statsList.stream().mapToLong(WebhookStatisticsResponse::getTotalDeliveries).sum();
        long failures = deliveryRepository.countByDeliveryStatus(WebhookStatus.FAILED);
        long retries = deliveryRepository.countByDeliveryStatus(WebhookStatus.RETRYING);
        long dlq = deliveryRepository.countByDeliveryStatus(WebhookStatus.DEAD_LETTER);

        long openCircuits = endpoints.stream()
                .filter(e -> e.getCircuitState() == CircuitState.OPEN)
                .count();

        long healthy = endpoints.stream()
                .filter(e -> e.isEnabled() && e.getCircuitState() == CircuitState.CLOSED)
                .count();

        long disabled = endpoints.stream()
                .filter(e -> !e.isEnabled())
                .count();

        Double avgLatency = deliveryRepository.averageExecutionTime();
        double avgLat = avgLatency != null ? avgLatency : 0.0;

        // Sort highlight endpoints
        List<WebhookStatisticsResponse> topFailed = statsList.stream()
                .filter(s -> s.getFailureCount() > 0)
                .sorted(Comparator.comparing(WebhookStatisticsResponse::getFailureCount).reversed())
                .limit(5)
                .collect(Collectors.toList());

        List<WebhookStatisticsResponse> slowest = statsList.stream()
                .filter(s -> s.getAverageLatencyMs() > 0)
                .sorted(Comparator.comparing(WebhookStatisticsResponse::getAverageLatencyMs).reversed())
                .limit(5)
                .collect(Collectors.toList());

        List<WebhookResponse> mostActive = endpoints.stream()
                .sorted(Comparator.comparing(this::getDeliveriesCountForEndpoint).reversed())
                .limit(5)
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        Map<String, Long> retryTrend = new HashMap<>();
        retryTrend.put("Today", retries);
        
        Map<String, Long> failureTrend = new HashMap<>();
        failureTrend.put("Today", failures + dlq);

        Map<String, Long> successTrend = new HashMap<>();
        successTrend.put("Today", deliveryRepository.countByDeliveryStatus(WebhookStatus.DELIVERED));

        return WebhookDashboardResponse.builder()
                .endpointsStats(statsList)
                .webhookQueueSize(deliveryRepository.countByDeliveryStatus(WebhookStatus.PENDING))
                .webhookRetryQueue(retries)
                .webhookDlq(dlq)
                .circuitOpenCount(openCircuits)
                .healthyEndpoints(healthy)
                .disabledEndpoints(disabled)
                .averageResponseTimeMs(avgLat)
                .averagePayloadSizeBytes(2048.0) // Mock/calculated baseline
                .topFailedEndpoints(topFailed)
                .slowestEndpoints(slowest)
                .mostActiveEndpoints(mostActive)
                .retryTrend(retryTrend)
                .failureTrend(failureTrend)
                .successTrend(successTrend)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public WebhookStatisticsResponse getEndpointStats(Long endpointId) {
        WebhookEndpoint endpoint = endpointRepository.findById(endpointId)
                .orElseThrow(() -> new IllegalArgumentException("Endpoint not found: " + endpointId));

        List<WebhookDelivery> deliveries = deliveryRepository.findRecentDeliveriesByEndpoint(endpointId, PageRequest.of(0, 1000));
        
        long total = deliveries.size();
        long success = 0;
        long failure = 0;
        long retry = 0;
        long timeout = 0;
        long deadLetter = 0;
        double totalLatency = 0.0;
        
        LocalDateTime lastSuccess = null;
        LocalDateTime lastFailure = null;

        for (WebhookDelivery d : deliveries) {
            if (d.getDeliveryStatus() == WebhookStatus.DELIVERED) {
                success++;
                if (lastSuccess == null || d.getCreatedAt().isAfter(lastSuccess)) {
                    lastSuccess = d.getCreatedAt();
                }
            } else if (d.getDeliveryStatus() == WebhookStatus.FAILED) {
                failure++;
                if (lastFailure == null || d.getCreatedAt().isAfter(lastFailure)) {
                    lastFailure = d.getCreatedAt();
                }
            } else if (d.getDeliveryStatus() == WebhookStatus.DEAD_LETTER) {
                deadLetter++;
                if (lastFailure == null || d.getCreatedAt().isAfter(lastFailure)) {
                    lastFailure = d.getCreatedAt();
                }
            } else if (d.getDeliveryStatus() == WebhookStatus.RETRYING) {
                retry++;
            }

            if (d.getFailureReason() != null && (d.getFailureReason().toLowerCase().contains("timeout") || d.getFailureReason().toLowerCase().contains("timed out"))) {
                timeout++;
            }

            if (d.getExecutionTimeMs() != null) {
                totalLatency += d.getExecutionTimeMs();
            }
        }

        double successPct = total == 0 ? 100.0 : ((double) success / total) * 100.0;
        double failurePct = total == 0 ? 0.0 : ((double) (failure + deadLetter) / total) * 100.0;
        double retryPct = total == 0 ? 0.0 : ((double) retry / total) * 100.0;
        double timeoutPct = total == 0 ? 0.0 : ((double) timeout / total) * 100.0;
        double avgLatency = total == 0 ? 0.0 : totalLatency / total;

        double healthScore = successPct;

        return WebhookStatisticsResponse.builder()
                .endpointId(endpointId)
                .name(endpoint.getName())
                .targetUrl(endpoint.getTargetUrl())
                .successPercentage(successPct)
                .failurePercentage(failurePct)
                .retryPercentage(retryPct)
                .timeoutPercentage(timeoutPct)
                .averageLatencyMs(avgLatency)
                .totalDeliveries(total)
                .successCount(success)
                .failureCount(failure + deadLetter)
                .retryCount(retry)
                .deadLetterCount(deadLetter)
                .healthScore(healthScore)
                .lastSuccess(lastSuccess)
                .lastFailure(lastFailure)
                .build();
    }

    private long getDeliveriesCountForEndpoint(WebhookEndpoint e) {
        return deliveryRepository.findRecentDeliveriesByEndpoint(e.getId(), PageRequest.of(0, 1000)).size();
    }

    private WebhookResponse mapToResponse(WebhookEndpoint endpoint) {
        return WebhookResponse.builder()
                .id(endpoint.getId())
                .name(endpoint.getName())
                .targetUrl(endpoint.getTargetUrl())
                .secretKey("********************")
                .enabled(endpoint.isEnabled())
                .timeoutMs(endpoint.getTimeoutMs())
                .retryEnabled(endpoint.isRetryEnabled())
                .maxRetryCount(endpoint.getMaxRetryCount())
                .subscribedEvents(endpoint.getSubscribedEvents())
                .webhookVersion(endpoint.getWebhookVersion())
                .requestsPerMinute(endpoint.getRequestsPerMinute())
                .requestsPerHour(endpoint.getRequestsPerHour())
                .batchEnabled(endpoint.isBatchEnabled())
                .batchSize(endpoint.getBatchSize())
                .batchIntervalSeconds(endpoint.getBatchIntervalSeconds())
                .compressionEnabled(endpoint.isCompressionEnabled())
                .circuitState(endpoint.getCircuitState())
                .consecutiveFailures(endpoint.getConsecutiveFailures())
                .lastFailureTime(endpoint.getLastFailureTime())
                .version(endpoint.getVersion() != null ? endpoint.getVersion().longValue() : null)
                .createdAt(endpoint.getCreatedAt())
                .updatedAt(endpoint.getUpdatedAt())
                .build();
    }
}
