package com.redis.security.dto.request;

import com.redis.common.entity.Permission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeyRequest {

    @NotBlank(message = "API key name is required")
    private String name;

    @NotEmpty(message = "At least one permission is required")
    private Set<Permission> permissions;

    private Integer expiresInDays;
}
