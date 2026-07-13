package com.redis.reliability.repository;

import com.redis.reliability.entity.BackupMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BackupMetadataRepository extends JpaRepository<BackupMetadata, Long> {
    List<BackupMetadata> findByStatus(String status);
}
