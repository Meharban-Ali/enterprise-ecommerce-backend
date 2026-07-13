package com.redis.auth.repository;

import com.redis.auth.entity.RefreshToken;
import com.redis.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // Find refresh token details
    Optional<RefreshToken> findByToken(String token);

    // Delete a specific token (used during specific device logout)
    void deleteByToken(String token);

    // Delete all tokens for a user (used during complete account revocation)
    void deleteByUser(User user);

    // Bulk delete expired tokens (used by Cron/Scheduler cleanups to prevent table bloat)
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < :now")
    int deleteExpiredTokens(@Param("now") Instant now);
}
