package com.redis.audit.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditSummaryResponse {
    private long totalEvents;
    private long successfulEvents;
    private long failedEvents;
    private long securityEvents;
    private long paymentEvents;
    private long orderEvents;
}
