package com.redis.security.repository;

import com.redis.security.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    Optional<ApiKey> findByName(String name);

    @Query("SELECT k FROM ApiKey k WHERE k.keyHash = :hash OR k.rotationKeyHash = :hash")
    Optional<ApiKey> findByKeyHashOrRotationKeyHash(@Param("hash") String hash);

    long countByEnabledTrueAndRevokedFalse();

    @Query("SELECT COUNT(k) FROM ApiKey k WHERE k.lockUntil IS NOT NULL AND k.lockUntil > :now")
    long countLockedKeys(@Param("now") java.time.LocalDateTime now);

    long countByRotationKeyHashIsNotNull();
}
