package com.redis.monitoring.repository;

import com.redis.monitoring.entity.AlertRule;
import com.redis.monitoring.entity.AlertSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {

    @Query("SELECT r FROM AlertRule r WHERE r.enabled = true")
    List<AlertRule> findEnabledRules();

    Optional<AlertRule> findByRuleCode(String ruleCode);

    List<AlertRule> findBySource(AlertSource source);
}
