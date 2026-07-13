package com.redis.notification.repository;

import com.redis.notification.entity.NotificationOutbox;
import com.redis.common.entity.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {

    List<NotificationOutbox> findTop100ByStatusOrderByCreatedAt(OutboxStatus status);

    Optional<NotificationOutbox> findByEventId(UUID eventId);

    long countByStatus(OutboxStatus status);

    @Query("SELECT o FROM NotificationOutbox o WHERE o.status = 'FAILED'")
    List<NotificationOutbox> findFailedEvents();

    @Modifying
    @Transactional
    @Query("UPDATE NotificationOutbox o SET o.status = 'PROCESSING' WHERE o.id = :id AND o.status = 'PENDING'")
    int claimEvent(@Param("id") Long id);
}
