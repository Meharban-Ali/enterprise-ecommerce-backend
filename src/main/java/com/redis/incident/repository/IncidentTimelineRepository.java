package com.redis.incident.repository;

import com.redis.incident.entity.IncidentTimeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentTimelineRepository extends JpaRepository<IncidentTimeline, Long> {
    List<IncidentTimeline> findByIncidentIdOrderByCreatedAtAsc(Long incidentId);
    List<IncidentTimeline> findByIncidentIncidentNumberOrderByCreatedAtAsc(String incidentNumber);
}
