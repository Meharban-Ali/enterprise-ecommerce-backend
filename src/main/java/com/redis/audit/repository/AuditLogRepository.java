package com.redis.audit.repository;

import com.redis.audit.entity.AuditLog;
import com.redis.audit.entity.AuditActionType;
import com.redis.audit.entity.AuditStatus;
import com.redis.common.entity.ResourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByUserId(Long userId, Pageable pageable);

    Page<AuditLog> findByActionType(AuditActionType actionType, Pageable pageable);

    Page<AuditLog> findByStatus(AuditStatus status, Pageable pageable);

    Page<AuditLog> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<AuditLog> findByResourceTypeAndResourceId(ResourceType resourceType, String resourceId, Pageable pageable);

    List<AuditLog> findByCorrelationId(String correlationId);

    long countByActionType(AuditActionType actionType);

    long countByStatus(AuditStatus status);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.actionType IN ('LOGIN', 'LOGIN_FAILED', 'LOGOUT', 'PASSWORD_RESET', 'ROLE_CHANGED', 'USER_DISABLED', 'ACCESS_DENIED')")
    long countSecurityEvents();

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.actionType IN ('PAYMENT_CREATED', 'PAYMENT_SUCCESS', 'PAYMENT_FAILED', 'PAYMENT_REFUND', 'PAYMENT_CANCELLED')")
    long countPaymentEvents();

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.actionType IN ('ORDER_CREATED', 'ORDER_UPDATED', 'ORDER_CANCELLED', 'ORDER_EXPIRED', 'ORDER_SHIPPED', 'ORDER_DELIVERED')")
    long countOrderEvents();
}
