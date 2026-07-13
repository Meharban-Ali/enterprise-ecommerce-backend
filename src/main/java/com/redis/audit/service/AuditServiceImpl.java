package com.redis.audit.service;

import com.redis.common.entity.ResourceType;
import com.redis.audit.entity.AuditStatus;

import com.redis.audit.dto.response.AuditLogResponse;
import com.redis.audit.dto.request.AuditSearchRequest;
import com.redis.audit.dto.response.AuditSummaryResponse;
import com.redis.audit.entity.AuditLog;
import com.redis.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Writer;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    private static final int MAX_PAGE_SIZE = 100;

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> searchLogs(AuditSearchRequest request, Pageable pageable) {
        log.info("Searching compliance audit logs");
        Pageable restrictedPageable = restrictPageSize(pageable);

        Page<AuditLog> result;
        if (request.getStartDate() != null && request.getEndDate() != null) {
            result = auditLogRepository.findByCreatedAtBetween(request.getStartDate(), request.getEndDate(), restrictedPageable);
        } else if (request.getUserId() != null) {
            result = auditLogRepository.findByUserId(request.getUserId(), restrictedPageable);
        } else if (request.getActionType() != null) {
            result = auditLogRepository.findByActionType(request.getActionType(), restrictedPageable);
        } else if (request.getStatus() != null) {
            result = auditLogRepository.findByStatus(request.getStatus(), restrictedPageable);
        } else if (request.getResourceType() != null && request.getResourceId() != null) {
            result = auditLogRepository.findByResourceTypeAndResourceId(request.getResourceType(), request.getResourceId(), restrictedPageable);
        } else {
            result = auditLogRepository.findAll(restrictedPageable);
        }

        return result.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditSummaryResponse getAuditSummary() {
        log.info("Retrieving audit statistics summary");
        return AuditSummaryResponse.builder()
                .totalEvents(auditLogRepository.count())
                .successfulEvents(auditLogRepository.countByStatus(com.redis.audit.entity.AuditStatus.SUCCESS))
                .failedEvents(auditLogRepository.countByStatus(com.redis.audit.entity.AuditStatus.FAILED))
                .securityEvents(auditLogRepository.countSecurityEvents())
                .paymentEvents(auditLogRepository.countPaymentEvents())
                .orderEvents(auditLogRepository.countOrderEvents())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getUserAuditHistory(Long userId, Pageable pageable) {
        log.info("Retrieving audit history for user ID: {}", userId);
        Pageable restrictedPageable = restrictPageSize(pageable);
        return auditLogRepository.findByUserId(userId, restrictedPageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getLogsByCorrelationId(String correlationId) {
        log.info("Retrieving audit history for correlation ID: {}", correlationId);
        return auditLogRepository.findByCorrelationId(correlationId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public void exportLogsToCsv(AuditSearchRequest request, Writer writer) {
        log.info("Exporting audit logs to CSV");
        Pageable firstPage = PageRequest.of(0, MAX_PAGE_SIZE);
        Page<AuditLogResponse> logs = searchLogs(request, firstPage);

        try {
            writer.write("ID,EventID,CorrelationID,RequestID,UserID,Email,ActionType,Status,ResourceType,ResourceId,Description,IPAddress,UserAgent,RequestURI,HTTPMethod,ActorType,RoleName,SessionID,AuthenticationMethod,ClientType,HTTPStatus,ExecutionTimeMs,CreatedAt\n");
            for (AuditLogResponse log : logs.getContent()) {
                writer.write(String.format("%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,\"%s\",%s,\"%s\",%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                        log.getId(),
                        log.getEventId(),
                        log.getCorrelationId(),
                        log.getRequestId() != null ? log.getRequestId() : "",
                        log.getUserId() != null ? String.valueOf(log.getUserId()) : "",
                        log.getEmail() != null ? log.getEmail() : "",
                        log.getActionType().name(),
                        log.getStatus().name(),
                        log.getResourceType().name(),
                        log.getResourceId() != null ? log.getResourceId() : "",
                        log.getDescription() != null ? log.getDescription().replace("\"", "\"\"") : "",
                        log.getIpAddress() != null ? log.getIpAddress() : "",
                        log.getUserAgent() != null ? log.getUserAgent().replace("\"", "\"\"") : "",
                        log.getRequestUri() != null ? log.getRequestUri() : "",
                        log.getHttpMethod() != null ? log.getHttpMethod() : "",
                        log.getActorType() != null ? log.getActorType() : "",
                        log.getRoleName() != null ? log.getRoleName() : "",
                        log.getSessionId() != null ? log.getSessionId() : "",
                        log.getAuthenticationMethod() != null ? log.getAuthenticationMethod() : "",
                        log.getClientType() != null ? log.getClientType() : "",
                        log.getHttpStatus() != null ? String.valueOf(log.getHttpStatus()) : "",
                        log.getExecutionTimeMs() != null ? String.valueOf(log.getExecutionTimeMs()) : "",
                        log.getCreatedAt().toString()
                ));
            }
            writer.flush();
        } catch (Exception e) {
            log.error("Failed to write CSV audit export: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to export audit logs to CSV", e);
        }
    }

    private Pageable restrictPageSize(Pageable pageable) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            log.warn("Requested page size {} exceeds maximum allowable limit {}. Restricting to maximum limit.",
                    pageable.getPageSize(), MAX_PAGE_SIZE);
            return PageRequest.of(pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort());
        }
        return pageable;
    }

    private AuditLogResponse mapToResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .eventId(log.getEventId())
                .correlationId(log.getCorrelationId())
                .requestId(log.getRequestId())
                .userId(log.getUserId())
                .email(log.getEmail())
                .actionType(log.getActionType())
                .status(log.getStatus())
                .resourceType(log.getResourceType())
                .resourceId(log.getResourceId())
                .description(log.getDescription())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .requestUri(log.getRequestUri())
                .httpMethod(log.getHttpMethod())
                .actorType(log.getActorType())
                .roleName(log.getRoleName())
                .sessionId(log.getSessionId())
                .authenticationMethod(log.getAuthenticationMethod())
                .clientType(log.getClientType())
                .httpStatus(log.getHttpStatus())
                .executionTimeMs(log.getExecutionTimeMs())
                .entityVersion(log.getEntityVersion())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
