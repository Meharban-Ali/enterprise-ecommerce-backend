package com.redis.incident.repository;

import com.redis.incident.entity.IncidentComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentCommentRepository extends JpaRepository<IncidentComment, Long> {
    List<IncidentComment> findByIncidentIdOrderByCreatedAtDesc(Long incidentId);
    List<IncidentComment> findByIncidentIncidentNumberOrderByCreatedAtDesc(String incidentNumber);
}
