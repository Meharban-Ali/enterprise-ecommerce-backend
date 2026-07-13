package com.redis.webhook.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookTestResponse {
    private boolean success;
    private int responseStatus;
    private String responseBody;
    private long executionTimeMs;
    private String error;
}
