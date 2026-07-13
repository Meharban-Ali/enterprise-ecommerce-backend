package com.redis.webhook.repository;

import com.redis.webhook.entity.WebhookDelivery;
import com.redis.common.entity.IntegrationEventType;
import com.redis.webhook.entity.WebhookStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, Long> {

    @Query("SELECT d FROM WebhookDelivery d WHERE d.deliveryStatus = 'RETRYING' OR (d.deliveryStatus = 'FAILED' AND d.retryCount < 5)")
    List<WebhookDelivery> findRetryCandidates();

    List<WebhookDelivery> findByCorrelationId(String correlationId);

    List<WebhookDelivery> findByDeliveryStatus(WebhookStatus status);

    @Query("SELECT d FROM WebhookDelivery d WHERE d.deliveryStatus = 'DEAD_LETTER'")
    List<WebhookDelivery> findDeadLetters();

    long countByDeliveryStatus(WebhookStatus status);

    long countByEventType(IntegrationEventType eventType);

    @Query("SELECT AVG(d.executionTimeMs) FROM WebhookDelivery d WHERE d.executionTimeMs IS NOT NULL")
    Double averageExecutionTime();

    @Query("SELECT d FROM WebhookDelivery d WHERE d.webhookEndpoint.id = :endpointId ORDER BY d.createdAt DESC")
    List<WebhookDelivery> findRecentDeliveriesByEndpoint(@Param("endpointId") Long endpointId, Pageable pageable);

    @Query("SELECT d FROM WebhookDelivery d WHERE d.deliveryStatus = 'FAILED' OR d.deliveryStatus = 'DEAD_LETTER'")
    List<WebhookDelivery> findFailedDeliveries();

    @Query("SELECT d FROM WebhookDelivery d WHERE d.aggregateType = :aggType AND d.aggregateId = :aggId AND d.deliveryStatus = 'PENDING' ORDER BY d.createdAt ASC")
    List<WebhookDelivery> findPendingDeliveriesForAggregate(@Param("aggType") String aggType, @Param("aggId") String aggId);

    Optional<WebhookDelivery> findByIdempotencyKey(String idempotencyKey);
}
