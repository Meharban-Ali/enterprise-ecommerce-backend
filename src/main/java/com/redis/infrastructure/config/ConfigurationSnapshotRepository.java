package com.redis.infrastructure.config;

import com.redis.infrastructure.config.ConfigurationSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ConfigurationSnapshotRepository extends JpaRepository<ConfigurationSnapshot, Long> {

    @Query("SELECT c FROM ConfigurationSnapshot c ORDER BY c.version DESC LIMIT 1")
    Optional<ConfigurationSnapshot> findLatestSnapshot();
}
