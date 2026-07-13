package com.redis.security.dto.response;

import com.redis.common.entity.Permission;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeyResponse {

    private Long id;
    private String name;
    private String apiKey; // Only populated on creation/rotation
    private Set<Permission> permissions;
    private boolean enabled;
    private boolean revoked;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Analytics summary
    private Long totalRequests;
    private Long failedRequests;
    private Double averageLatencyMs;
    private LocalDateTime lastUsedTime;
    private String lastIpAddress;
    private Long rateLimitViolations;

    private LocalDateTime lastSuccessfulAuthentication;
    private Integer failedAuthenticationCount;
    private LocalDateTime lockUntil;
    private Long requestsPerHour;
    private Long requestsPerDay;
    private Double errorRate;
    private Double successRate;
    private Integer peakUsageHour;
    private java.util.Map<String, Long> topEndpoints;
}
