package com.redis.infrastructure.governance.service;

import com.redis.infrastructure.governance.dto.ApiGovernanceDashboardResponse;
import com.redis.security.dto.response.ApiKeyResponse;
import com.redis.security.entity.ApiKey;
import com.redis.security.repository.ApiKeyRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ApiGovernanceServiceImpl implements ApiGovernanceService {

    @Autowired(required = false)
    private ApiKeyRepository apiKeyRepository;

    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong rateLimitViolations = new AtomicLong(0);
    private final AtomicLong idempotentHits = new AtomicLong(0);
    private final AtomicLong replayAttacksBlocked = new AtomicLong(0);
    private final AtomicLong validationFailures = new AtomicLong(0);
    private final AtomicLong totalPayloadSize = new AtomicLong(0);
    private final AtomicLong totalResponseSize = new AtomicLong(0);
    private final AtomicLong maxResponseSize = new AtomicLong(0);
    
    private final Map<String, AtomicLong> errorDistribution = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> apiCallCounts = new ConcurrentHashMap<>();
    private final Map<String, ConcurrentLinkedQueue<Long>> apiLatencies = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> consumerCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> apiKeyUsageCounts = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Long> allRequestLatencies = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Long> requestTimestamps = new ConcurrentLinkedQueue<>();

    private final ConcurrentLinkedQueue<RequestMetrics> metricsQueue = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "governance-analytics-worker");
        t.setDaemon(true);
        return t;
    });

    @PostConstruct
    public void startAsyncProcessor() {
        scheduler.scheduleWithFixedDelay(this::processBatch, 100, 100, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void stopAsyncProcessor() {
        scheduler.shutdown();
    }

    @Override
    public void recordRequest(String uri, String method, long latencyMs, int statusCode, long payloadSize, long responseSize,
                       String consumerKey, boolean isIdempotencyHit, boolean isReplayAttack,
                       boolean isRateLimitExceeded, boolean isValidationError) {
        metricsQueue.add(new RequestMetrics(uri, method, latencyMs, statusCode, payloadSize, responseSize,
                consumerKey, isIdempotencyHit, isReplayAttack, isRateLimitExceeded, isValidationError));
    }

    private void processBatch() {
        RequestMetrics metrics;
        while ((metrics = metricsQueue.poll()) != null) {
            long now = System.currentTimeMillis();
            totalRequests.incrementAndGet();
            requestTimestamps.add(now);
            allRequestLatencies.add(metrics.latencyMs);
            totalPayloadSize.addAndGet(metrics.payloadSize);
            totalResponseSize.addAndGet(metrics.responseSize);

            long currentMax;
            do {
                currentMax = maxResponseSize.get();
                if (metrics.responseSize <= currentMax) {
                    break;
                }
            } while (!maxResponseSize.compareAndSet(currentMax, metrics.responseSize));

            if (metrics.statusCode >= 200 && metrics.statusCode < 400) {
                successfulRequests.incrementAndGet();
            } else {
                String errCode = String.valueOf(metrics.statusCode);
                errorDistribution.computeIfAbsent(errCode, k -> new AtomicLong(0)).incrementAndGet();
            }

            if (metrics.isIdempotencyHit) idempotentHits.incrementAndGet();
            if (metrics.isReplayAttack) replayAttacksBlocked.incrementAndGet();
            if (metrics.isRateLimitExceeded) rateLimitViolations.incrementAndGet();
            if (metrics.isValidationError) validationFailures.incrementAndGet();

            // API metrics
            String apiKey = metrics.method + " " + metrics.uri;
            apiCallCounts.computeIfAbsent(apiKey, k -> new AtomicLong(0)).incrementAndGet();
            apiLatencies.computeIfAbsent(apiKey, k -> new ConcurrentLinkedQueue<>()).add(metrics.latencyMs);

            // Consumer metrics
            if (metrics.consumerKey != null) {
                consumerCounts.computeIfAbsent(metrics.consumerKey, k -> new AtomicLong(0)).incrementAndGet();
                if (metrics.consumerKey.startsWith("key:")) {
                    apiKeyUsageCounts.computeIfAbsent(metrics.consumerKey.substring(4), k -> new AtomicLong(0)).incrementAndGet();
                }
            }
        }
    }

    @Override
    public ApiGovernanceDashboardResponse getDashboard() {
        // Force processing of remaining batch for real-time accuracy on request
        processBatch();

        long now = System.currentTimeMillis();
        long oneMinuteAgo = now - 60000;

        while (!requestTimestamps.isEmpty() && requestTimestamps.peek() < oneMinuteAgo) {
            requestTimestamps.poll();
        }
        double rpm = requestTimestamps.size();

        long total = totalRequests.get();
        double successRate = total == 0 ? 100.0 : (successfulRequests.get() * 100.0) / total;

        List<Long> latencies = new ArrayList<>(allRequestLatencies);
        Collections.sort(latencies);

        double avgLatency = 0.0;
        double p95 = 0.0;
        double p99 = 0.0;

        if (!latencies.isEmpty()) {
            avgLatency = latencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
            p95 = latencies.get((int) (latencies.size() * 0.95));
            p99 = latencies.get((int) (latencies.size() * 0.99));
        }

        Map<String, Double> slowestApis = new HashMap<>();
        apiLatencies.forEach((api, queue) -> {
            List<Long> list = new ArrayList<>(queue);
            double avg = list.stream().mapToLong(Long::longValue).average().orElse(0.0);
            slowestApis.put(api, avg);
        });

        Map<String, Double> topSlowest = slowestApis.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        Map<String, Long> topCalled = apiCallCounts.entrySet().stream()
                .sorted(Map.Entry.<String, AtomicLong>comparingByValue(Comparator.comparingLong(AtomicLong::get)).reversed())
                .limit(5)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(), (e1, e2) -> e1, LinkedHashMap::new));

        Map<String, Long> topConsumers = consumerCounts.entrySet().stream()
                .sorted(Map.Entry.<String, AtomicLong>comparingByValue(Comparator.comparingLong(AtomicLong::get)).reversed())
                .limit(5)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(), (e1, e2) -> e1, LinkedHashMap::new));

        Map<String, Long> errors = errorDistribution.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));

        Map<String, Long> keyUsage = apiKeyUsageCounts.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));

        double avgPayloadSize = total == 0 ? 0.0 : (double) totalPayloadSize.get() / total;
        double avgResponseSize = total == 0 ? 0.0 : (double) totalResponseSize.get() / total;

        long activeKeys = 0;
        long lockedKeys = 0;
        long rotatedKeysCount = 0;
        Map<String, ApiKeyResponse> consumerAnalyticsMap = new HashMap<>();

        if (apiKeyRepository != null) {
            try {
                activeKeys = apiKeyRepository.countByEnabledTrueAndRevokedFalse();
                lockedKeys = apiKeyRepository.countLockedKeys(LocalDateTime.now());
                rotatedKeysCount = apiKeyRepository.countByRotationKeyHashIsNotNull();

                List<ApiKey> allKeys = apiKeyRepository.findAll();
                for (ApiKey key : allKeys) {
                    ApiKeyResponse resp = mapToResponse(key);
                    consumerAnalyticsMap.put(key.getName(), resp);
                }
            } catch (Exception e) {
                log.error("Failed to query API Key analytics for dashboard", e);
            }
        }

        return ApiGovernanceDashboardResponse.builder()
                .totalRequests(total)
                .successRate(successRate)
                .errorDistribution(errors)
                .averageResponseTimeMs(avgLatency)
                .p95LatencyMs(p95)
                .p99LatencyMs(p99)
                .slowestApis(topSlowest)
                .mostCalledApis(topCalled)
                .rateLimitViolations(rateLimitViolations.get())
                .idempotentHits(idempotentHits.get())
                .replayAttacksBlocked(replayAttacksBlocked.get())
                .validationFailures(validationFailures.get())
                .averagePayloadSizeBytes(avgPayloadSize)
                .requestsPerMinute(rpm)
                .topConsumers(topConsumers)
                .apiKeyUsage(keyUsage)
                .activeApiKeys(activeKeys)
                .lockedApiKeys(lockedKeys)
                .rotatedKeys(rotatedKeysCount)
                .averageResponseSizeBytes(avgResponseSize)
                .maxResponseSizeBytes(maxResponseSize.get())
                .endpointLatencyRanking(topSlowest)
                .consumerAnalytics(consumerAnalyticsMap)
                .build();
    }

    private ApiKeyResponse mapToResponse(ApiKey apiKey) {
        return ApiKeyResponse.builder()
                .id(apiKey.getId())
                .name(apiKey.getName())
                .permissions(apiKey.getPermissions())
                .enabled(apiKey.isEnabled())
                .revoked(apiKey.isRevoked())
                .expiresAt(apiKey.getExpiresAt())
                .createdAt(apiKey.getCreatedAt())
                .updatedAt(apiKey.getUpdatedAt())
                .totalRequests(apiKey.getTotalRequests())
                .failedRequests(apiKey.getFailedRequests())
                .averageLatencyMs(apiKey.getAverageLatencyMs())
                .lastUsedTime(apiKey.getLastUsedTime())
                .lastIpAddress(apiKey.getLastIpAddress())
                .rateLimitViolations(apiKey.getRateLimitViolations())
                .lastSuccessfulAuthentication(apiKey.getLastSuccessfulAuthentication())
                .failedAuthenticationCount(apiKey.getFailedAuthenticationCount())
                .lockUntil(apiKey.getLockUntil())
                .requestsPerHour(apiKey.getRequestsPerHour())
                .requestsPerDay(apiKey.getRequestsPerDay())
                .errorRate(apiKey.getErrorRate())
                .successRate(apiKey.getSuccessRate())
                .peakUsageHour(apiKey.getPeakUsageHour())
                .topEndpoints(parseTopEndpoints(apiKey.getTopEndpointsJson()))
                .build();
    }

    private java.util.Map<String, Long> parseTopEndpoints(String json) {
        java.util.Map<String, Long> map = new java.util.HashMap<>();
        if (json == null || json.isEmpty()) {
            return map;
        }
        try {
            String[] entries = json.split(",");
            for (String entry : entries) {
                String[] parts = entry.split(":");
                if (parts.length == 2) {
                    map.put(parts[0], Long.parseLong(parts[1]));
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return map;
    }

    private static class RequestMetrics {
        final String uri;
        final String method;
        final long latencyMs;
        final int statusCode;
        final long payloadSize;
        final long responseSize;
        final String consumerKey;
        final boolean isIdempotencyHit;
        final boolean isReplayAttack;
        final boolean isRateLimitExceeded;
        final boolean isValidationError;

        RequestMetrics(String uri, String method, long latencyMs, int statusCode, long payloadSize, long responseSize,
                       String consumerKey, boolean isIdempotencyHit, boolean isReplayAttack,
                       boolean isRateLimitExceeded, boolean isValidationError) {
            this.uri = uri;
            this.method = method;
            this.latencyMs = latencyMs;
            this.statusCode = statusCode;
            this.payloadSize = payloadSize;
            this.responseSize = responseSize;
            this.consumerKey = consumerKey;
            this.isIdempotencyHit = isIdempotencyHit;
            this.isReplayAttack = isReplayAttack;
            this.isRateLimitExceeded = isRateLimitExceeded;
            this.isValidationError = isValidationError;
        }
    }
}
