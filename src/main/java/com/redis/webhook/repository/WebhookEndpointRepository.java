package com.redis.webhook.repository;

import com.redis.webhook.entity.WebhookEndpoint;
import com.redis.common.entity.IntegrationEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WebhookEndpointRepository extends JpaRepository<WebhookEndpoint, Long> {

    @Query("SELECT e FROM WebhookEndpoint e WHERE e.enabled = true")
    List<WebhookEndpoint> findEnabledEndpoints();

    @Query("SELECT e FROM WebhookEndpoint e WHERE e.enabled = true AND :eventType MEMBER OF e.subscribedEvents")
    List<WebhookEndpoint> findByEventType(@Param("eventType") IntegrationEventType eventType);

    Optional<WebhookEndpoint> findByTargetUrl(String targetUrl);

    boolean existsByTargetUrl(String targetUrl);
}
