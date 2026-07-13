package com.redis.notification.repository;

import com.redis.notification.entity.NotificationTemplateEntity;
import com.redis.notification.entity.NotificationChannel;
import com.redis.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplateEntity, Long> {

    Optional<NotificationTemplateEntity> findByTemplateCodeAndActiveTrue(String templateCode);

    Optional<NotificationTemplateEntity> findByTemplateCodeAndVersion(String templateCode, int version);

    List<NotificationTemplateEntity> findByNotificationType(NotificationType notificationType);

    @Query("SELECT t FROM NotificationTemplateEntity t WHERE t.active = true")
    List<NotificationTemplateEntity> findActiveTemplates();

    boolean existsByTemplateCode(String templateCode);

    @Query("SELECT COALESCE(MAX(t.version), 0) FROM NotificationTemplateEntity t WHERE t.templateCode = :templateCode")
    int findMaxVersionByTemplateCode(@Param("templateCode") String templateCode);

    @Query("SELECT t FROM NotificationTemplateEntity t WHERE t.notificationType = :type AND t.notificationChannel = :channel AND t.active = true")
    Optional<NotificationTemplateEntity> findActiveByTypeAndChannel(@Param("type") NotificationType type, @Param("channel") NotificationChannel channel);
}
