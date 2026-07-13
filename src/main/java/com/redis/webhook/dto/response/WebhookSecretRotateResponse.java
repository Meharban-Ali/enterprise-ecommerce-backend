package com.redis.webhook.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookSecretRotateResponse {
    private Long endpointId;
    private String name;
    private String newSecretKey; // Masked or unmasked for this single operation so admin can copy it!
}
