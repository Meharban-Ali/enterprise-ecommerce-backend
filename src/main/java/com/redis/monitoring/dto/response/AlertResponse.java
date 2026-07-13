package com.redis.monitoring.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponse {
    private String ruleName;
    private String severity; // WARNING, CRITICAL
    private String message;
    private LocalDateTime timestamp;
}
