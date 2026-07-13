package com.redis.audit.service;

import com.redis.audit.dto.response.AuditLogResponse;
import com.redis.audit.dto.request.AuditSearchRequest;
import com.redis.audit.dto.response.AuditSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.Writer;
import java.util.List;

public interface AuditService {

    Page<AuditLogResponse> searchLogs(AuditSearchRequest request, Pageable pageable);

    AuditSummaryResponse getAuditSummary();

    Page<AuditLogResponse> getUserAuditHistory(Long userId, Pageable pageable);

    List<AuditLogResponse> getLogsByCorrelationId(String correlationId);

    void exportLogsToCsv(AuditSearchRequest request, Writer writer);
}
