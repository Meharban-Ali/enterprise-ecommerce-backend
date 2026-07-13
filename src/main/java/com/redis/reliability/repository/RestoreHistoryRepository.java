package com.redis.reliability.repository;

import com.redis.reliability.entity.RestoreHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RestoreHistoryRepository extends JpaRepository<RestoreHistory, Long> {
}
