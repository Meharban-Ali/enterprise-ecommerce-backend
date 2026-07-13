package com.redis.incident.repository;

import com.redis.incident.entity.Incident;
import com.redis.monitoring.entity.AlertSeverity;
import com.redis.monitoring.entity.AlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long>, JpaSpecificationExecutor<Incident> {

    @Query("SELECT i FROM Incident i WHERE i.status = 'OPEN' OR i.status = 'ACKNOWLEDGED'")
    List<Incident> findOpenIncidents();

    List<Incident> findByStatus(AlertStatus status);

    List<Incident> findBySeverity(AlertSeverity severity);

    Optional<Incident> findByIncidentNumber(String incidentNumber);

    long countByStatus(AlertStatus status);

    long countBySeverity(AlertSeverity severity);

    Optional<Incident> findTopByAlertRuleIdOrderByCreatedAtDesc(Long ruleId);

    @Query("SELECT i FROM Incident i ORDER BY i.createdAt DESC")
    List<Incident> findRecentIncidents(Pageable pageable);

    @Query("SELECT i FROM Incident i WHERE i.status = 'RESOLVED' OR i.status = 'CLOSED'")
    List<Incident> findResolvedOrClosedIncidents();
}
