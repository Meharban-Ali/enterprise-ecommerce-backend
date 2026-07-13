package com.redis.security.repository;

import com.redis.security.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyKey, Long> {

    Optional<IdempotencyKey> findByKey(String key);

    @Transactional
    @Modifying
    @Query("DELETE FROM IdempotencyKey ik WHERE ik.expiresAt <= :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}
