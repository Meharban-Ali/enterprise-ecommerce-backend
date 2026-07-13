package com.redis.reliability.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentSystemErrorResponse {
    private String component; // e.g. "SCHEDULER", "PAYMENT", "NOTIFICATION", "AUDIT"
    private String errorType; // e.g. "NullPointerException", "PaymentFailedException"
    private String message;
    private LocalDateTime timestamp;

    // Sprint 9.1 enhancements
    private String module;
    private String exceptionClass;
    private String rootCauseMessage;
    private String correlationId;
    private Long executionDurationMs;
    private String severity; // e.g. "INFO", "WARNING", "ERROR", "CRITICAL"
}
