package com.redis.notification.repository;

import com.redis.notification.entity.NotificationChannel;
import com.redis.notification.entity.Notification;
import com.redis.notification.entity.NotificationStatus;
import com.redis.notification.entity.NotificationType;
import com.redis.notification.entity.NotificationPriority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @EntityGraph(attributePaths = "user")
    Page<Notification> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<Notification> findByUserIdAndReadStatus(Long userId, boolean readStatus, Pageable pageable);

    long countByUserIdAndReadStatus(Long userId, boolean readStatus);

    @EntityGraph(attributePaths = "user")
    Page<Notification> findByStatus(NotificationStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<Notification> findByStatusAndCreatedAtBefore(NotificationStatus status, LocalDateTime time, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    List<Notification> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = "user")
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    @Query("SELECT n FROM Notification n WHERE n.status = :status AND (n.nextRetryAt IS NULL OR n.nextRetryAt <= :now)")
    Page<Notification> findRetryCandidates(
            @Param("status") NotificationStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    @EntityGraph(attributePaths = "user")
    @Query("SELECT n FROM Notification n WHERE n.status = :status AND (n.nextRetryAt IS NULL OR n.nextRetryAt <= :now)")
    List<Notification> findTopRetryCandidates(
            @Param("status") NotificationStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    @EntityGraph(attributePaths = "user")
    default Page<Notification> findDeadLetters(Pageable pageable) {
        return findByStatus(NotificationStatus.DEAD_LETTER, pageable);
    }

    long countByStatus(NotificationStatus status);

    long countByStatusAndCreatedAtBetween(NotificationStatus status, LocalDateTime start, LocalDateTime end);

    long countByChannel(NotificationChannel channel);

    long countByChannelAndCreatedAtBetween(NotificationChannel channel, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.status = :status AND n.retryCount > 0")
    long countByStatusAndRetryCountGreaterThanZero(@Param("status") NotificationStatus status);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.status = :status AND n.retryCount > 0 AND n.createdAt BETWEEN :start AND :end")
    long countByStatusAndRetryCountGreaterThanZeroBetween(
            @Param("status") NotificationStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.retryCount > 0")
    long countByRetryCountGreaterThanZero();

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.retryCount > 0 AND n.createdAt BETWEEN :start AND :end")
    long countByRetryCountGreaterThanZeroBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT AVG(CAST(n.retryCount AS double)) FROM Notification n")
    Double getAverageRetryCount();

    @Query("SELECT AVG(CAST(n.retryCount AS double)) FROM Notification n")
    Double averageRetryCount();

    @Query("SELECT AVG(CAST(n.retryCount AS double)) FROM Notification n WHERE n.createdAt BETWEEN :start AND :end")
    Double averageRetryCountBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT n.channel, COUNT(n) FROM Notification n GROUP BY n.channel")
    List<Object[]> countGroupedByChannel();

    @Query("SELECT n.createdAt, n.deliveredAt FROM Notification n WHERE n.deliveredAt IS NOT NULL")
    List<Object[]> getDeliveryTimestamps(Pageable pageable);

    @Query("SELECT n.createdAt, n.deliveredAt FROM Notification n WHERE n.deliveredAt IS NOT NULL")
    List<Object[]> getDeliveryTimestamps();

    @Query("SELECT n.createdAt, n.deliveredAt FROM Notification n WHERE n.deliveredAt IS NOT NULL AND n.createdAt BETWEEN :start AND :end")
    List<Object[]> getDeliveryTimestampsBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT n.createdAt, n.resolvedAt FROM Notification n WHERE n.resolvedAt IS NOT NULL")
    List<Object[]> getResolutionTimestamps(Pageable pageable);

    @Query("SELECT n.createdAt, n.resolvedAt FROM Notification n WHERE n.resolvedAt IS NOT NULL")
    List<Object[]> getResolutionTimestamps();

    @Query("SELECT n.createdAt, n.resolvedAt FROM Notification n WHERE n.resolvedAt IS NOT NULL AND n.createdAt BETWEEN :start AND :end")
    List<Object[]> getResolutionTimestampsBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT n.status, COUNT(n) FROM Notification n WHERE n.channel = :channel GROUP BY n.status")
    List<Object[]> countGroupedByStatusForChannel(@Param("channel") NotificationChannel channel);

    @Query("SELECT n.status, COUNT(n) FROM Notification n WHERE n.channel = :channel AND n.createdAt BETWEEN :start AND :end GROUP BY n.status")
    List<Object[]> countGroupedByStatusForChannelBetween(
            @Param("channel") NotificationChannel channel,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.type = :type AND n.status = 'SENT'")
    long countSentByType(@Param("type") NotificationType type);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.type = :type AND n.status = 'SENT' AND n.createdAt BETWEEN :start AND :end")
    long countSentByTypeBetween(
            @Param("type") NotificationType type,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.type = :type")
    long countTotalByType(@Param("type") NotificationType type);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.type = :type AND n.createdAt BETWEEN :start AND :end")
    long countTotalByTypeBetween(
            @Param("type") NotificationType type,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT n.priority, COUNT(n) FROM Notification n GROUP BY n.priority")
    List<Object[]> countGroupedByPriority();

    @Query("SELECT n.priority, COUNT(n) FROM Notification n WHERE n.createdAt BETWEEN :start AND :end GROUP BY n.priority")
    List<Object[]> countGroupedByPriorityBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @EntityGraph(attributePaths = "user")
    @Query("SELECT n FROM Notification n WHERE n.status = 'DEAD_LETTER' ORDER BY n.updatedAt DESC")
    List<Notification> findRecentDeadLetters(Pageable pageable);

    @EntityGraph(attributePaths = "user")
    @Query("SELECT n FROM Notification n WHERE n.status = 'DEAD_LETTER' AND n.createdAt BETWEEN :start AND :end ORDER BY n.updatedAt DESC")
    List<Notification> findRecentDeadLettersBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);

    @EntityGraph(attributePaths = "user")
    @Query("SELECT n FROM Notification n WHERE n.status = 'FAILED' ORDER BY n.updatedAt DESC")
    List<Notification> findRecentFailures(Pageable pageable);

    @EntityGraph(attributePaths = "user")
    @Query("SELECT n FROM Notification n WHERE n.status = 'FAILED' AND n.createdAt BETWEEN :start AND :end ORDER BY n.updatedAt DESC")
    List<Notification> findRecentFailuresBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);

    @EntityGraph(attributePaths = "user")
    @Query("SELECT n FROM Notification n WHERE n.status = 'RETRYING' ORDER BY n.updatedAt DESC")
    List<Notification> findRecentRetries(Pageable pageable);

    @EntityGraph(attributePaths = "user")
    @Query("SELECT n FROM Notification n WHERE n.status = 'RETRYING' AND n.createdAt BETWEEN :start AND :end ORDER BY n.updatedAt DESC")
    List<Notification> findRecentRetriesBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("UPDATE Notification n SET n.status = 'DELIVERING' WHERE n.id = :id AND (n.status = 'PENDING' OR n.status = 'FAILED' OR n.status = 'RETRYING')")
    int transitionToDelivering(@Param("id") Long id);

    @EntityGraph(attributePaths = "user")
    @Query("SELECT n FROM Notification n WHERE n.id = :id")
    Optional<Notification> findByIdWithUser(@Param("id") Long id);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.status = :status AND n.retryCount >= :retryCount")
    long countByStatusAndRetryCountGreaterThanEqual(
            @Param("status") NotificationStatus status,
            @Param("retryCount") int retryCount
    );
}
