package com.redis.audit.dto.request;

import com.redis.audit.entity.AuditActionType;
import com.redis.audit.entity.AuditStatus;
import com.redis.common.entity.ResourceType;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditSearchRequest {
    private Long userId;
    private String email;
    private AuditActionType actionType;
    private AuditStatus status;
    private ResourceType resourceType;
    private String resourceId;
    private String correlationId;
    private String requestId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
