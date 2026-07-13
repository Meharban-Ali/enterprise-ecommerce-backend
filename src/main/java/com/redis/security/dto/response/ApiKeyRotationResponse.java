package com.redis.security.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeyRotationResponse {
    private Long id;
    private String name;
    private String newApiKey;
    private LocalDateTime gracePeriodExpiresAt;
}
