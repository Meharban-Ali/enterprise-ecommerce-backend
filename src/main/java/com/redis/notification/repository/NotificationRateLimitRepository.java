package com.redis.notification.repository;

import com.redis.notification.entity.NotificationRateLimit;
import com.redis.notification.entity.NotificationChannel;
import com.redis.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface NotificationRateLimitRepository extends JpaRepository<NotificationRateLimit, Long> {

    Optional<NotificationRateLimit> findByUserIdAndNotificationTypeAndChannelAndWindowType(
            Long userId, NotificationType notificationType, NotificationChannel channel, String windowType);

    @Modifying
    @Transactional
    @Query("DELETE FROM NotificationRateLimit r WHERE r.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM NotificationRateLimit r WHERE r.windowStart < :expiryTime")
    void deleteExpiredWindows(@Param("expiryTime") LocalDateTime expiryTime);
}
