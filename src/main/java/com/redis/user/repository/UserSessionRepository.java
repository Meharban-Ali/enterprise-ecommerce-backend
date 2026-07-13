package com.redis.user.repository;

import com.redis.user.entity.UserSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findByUserId(Long userId);

    Optional<UserSession> findByEmail(String email);

    /**
     * Currently Logged-In Users: status is 'ONLINE' and last activity is after threshold time.
     */
    @Query("SELECT s FROM UserSession s WHERE s.status = 'ONLINE' AND s.lastActivity >= :threshold")
    Page<UserSession> findOnlineSessions(@Param("threshold") LocalDateTime threshold, Pageable pageable);

    /**
     * Recently Logged-Out/Inactive Users: status is 'OFFLINE' OR last activity is before threshold time.
     */
    @Query("SELECT s FROM UserSession s WHERE s.status = 'OFFLINE' OR s.lastActivity < :threshold")
    Page<UserSession> findOfflineSessions(@Param("threshold") LocalDateTime threshold, Pageable pageable);

    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.status = 'ONLINE' AND s.lastActivity >= :threshold")
    long countOnlineSessions(@Param("threshold") LocalDateTime threshold);
}
