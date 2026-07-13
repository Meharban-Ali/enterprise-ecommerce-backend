package com.redis.incident.service;

import com.redis.incident.dto.response.IncidentDashboardResponse;
import com.redis.category.entity.ResolutionCategory;
import com.redis.monitoring.entity.AlertStatus;
import com.redis.incident.entity.Incident;
import com.redis.incident.dto.request.IncidentCommentRequest;
import com.redis.monitoring.entity.AlertSource;
import com.redis.incident.entity.IncidentTimeline;
import com.redis.monitoring.repository.AlertRuleRepository;
import com.redis.monitoring.entity.AlertSeverity;
import com.redis.incident.dto.response.IncidentCommentResponse;
import com.redis.incident.dto.response.IncidentSummaryResponse;
import com.redis.incident.repository.IncidentCommentRepository;
import com.redis.incident.dto.response.IncidentResponse;
import com.redis.common.entity.EscalationLevel;
import com.redis.monitoring.entity.AlertRule;
import com.redis.incident.repository.IncidentTimelineRepository;
import com.redis.incident.repository.IncidentRepository;

import com.redis.infrastructure.config.TestRedisConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@Transactional
public class IncidentServiceIntegrationTest {

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private AlertRuleRepository alertRuleRepository;

    @Autowired
    private IncidentTimelineRepository incidentTimelineRepository;

    @Autowired
    private IncidentCommentRepository incidentCommentRepository;

    private AlertRule testRule;

    @BeforeEach
    void setUp() {
        incidentCommentRepository.deleteAllInBatch();
        incidentTimelineRepository.deleteAllInBatch();
        incidentRepository.deleteAllInBatch();
        alertRuleRepository.deleteAllInBatch();

        testRule = AlertRule.builder()
                .ruleCode("TEST_RULE")
                .ruleName("Test Evaluation Rule")
                .source(AlertSource.SYSTEM)
                .severity(AlertSeverity.HIGH)
                .threshold(10.0)
                .enabled(true)
                .evaluationIntervalSeconds(10)
                .cooldownSeconds(60)
                .notificationEnabled(true)
                .updatedBy("TEST_USER")
                .build();
        testRule = alertRuleRepository.save(testRule);
    }

    @Test
    void testIncidentTimelineAndLifecycle() {
        Incident incident = Incident.builder()
                .incidentNumber("INC-2026-999999")
                .alertRule(testRule)
                .severity(AlertSeverity.HIGH)
                .source(AlertSource.SYSTEM)
                .title("Test Incident")
                .description("Test description")
                .status(AlertStatus.OPEN)
                .firstOccurredAt(LocalDateTime.now())
                .lastOccurredAt(LocalDateTime.now())
                .occurrenceCount(1)
                .slaDeadline(LocalDateTime.now().plusHours(4))
                .slaBreached(false)
                .acknowledgementDeadline(LocalDateTime.now().plusMinutes(30))
                .escalationLevel(EscalationLevel.L1)
                .build();
        incident = incidentRepository.save(incident);

        // Timeline check on create
        IncidentTimeline createTimeline = IncidentTimeline.builder()
                .incident(incident)
                .previousStatus(null)
                .newStatus(AlertStatus.OPEN)
                .previousSeverity(null)
                .newSeverity(AlertSeverity.HIGH)
                .actionPerformedBy("SYSTEM")
                .actionSource("SYSTEM")
                .remarks("Created")
                .build();
        incidentTimelineRepository.save(createTimeline);

        // 1. Acknowledge
        IncidentResponse acked = incidentService.acknowledge(incident.getId(), "admin_user");
        assertEquals(AlertStatus.ACKNOWLEDGED, acked.getStatus());
        assertEquals("admin_user", acked.getAcknowledgedBy());
        assertNotNull(acked.getAcknowledgedAt());

        List<IncidentTimeline> timelineList = incidentTimelineRepository.findByIncidentIdOrderByCreatedAtAsc(incident.getId());
        // Should have 2 entries (create + ack)
        assertTrue(timelineList.size() >= 2);
        assertEquals(AlertStatus.ACKNOWLEDGED, timelineList.get(timelineList.size() - 1).getNewStatus());

        // 2. Resolve
        IncidentResponse resolved = incidentService.resolve(
                incident.getId(), "admin_user", "Database connection pool exhausted", "Restarted DB services", ResolutionCategory.DATABASE
        );
        assertEquals(AlertStatus.RESOLVED, resolved.getStatus());
        assertEquals("admin_user", resolved.getResolvedBy());
        assertEquals("Database connection pool exhausted", resolved.getRootCause());
        assertEquals(ResolutionCategory.DATABASE, resolved.getResolutionCategory());
        assertFalse(resolved.isSlaBreached());

        // 3. Close
        IncidentResponse closed = incidentService.close(incident.getId(), "admin_user");
        assertEquals(AlertStatus.CLOSED, closed.getStatus());
        assertEquals("admin_user", closed.getClosedBy());

        // 4. Reopen
        IncidentResponse reopened = incidentService.reopen(incident.getId(), "admin_user");
        assertEquals(AlertStatus.REOPENED, reopened.getStatus());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "support_agent")
    void testIncidentComments() {
        Incident incident = createTestIncident("INC-COMMENT-001");

        IncidentCommentRequest req = IncidentCommentRequest.builder()
                .comment("Investigating the database latency...")
                .build();

        IncidentCommentResponse commentRes = incidentService.addComment(incident.getId(), req, "support_agent");
        assertEquals("Investigating the database latency...", commentRes.getComment());
        assertEquals("support_agent", commentRes.getCreatedBy());

        List<IncidentCommentResponse> comments = incidentService.getIncidentComments(incident.getIncidentNumber());
        assertEquals(1, comments.size());
        assertEquals("support_agent", comments.get(0).getCreatedBy());
    }

    @Test
    void testIncidentSlaTracking() {
        LocalDateTime pastDeadline = LocalDateTime.now().minusHours(1);
        Incident incident = Incident.builder()
                .incidentNumber("INC-SLA-001")
                .alertRule(testRule)
                .severity(AlertSeverity.CRITICAL)
                .source(AlertSource.DATABASE)
                .title("SLA Test")
                .description("SLA Description")
                .status(AlertStatus.OPEN)
                .firstOccurredAt(LocalDateTime.now().minusHours(3))
                .lastOccurredAt(LocalDateTime.now())
                .occurrenceCount(1)
                .slaDeadline(pastDeadline)
                .slaBreached(false)
                .acknowledgementDeadline(LocalDateTime.now().minusHours(2))
                .escalationLevel(EscalationLevel.L1)
                .build();
        incident = incidentRepository.save(incident);

        // Resolve after deadline breaches SLA
        IncidentResponse res = incidentService.resolve(
                incident.getId(), "admin", "Slow disk", "Swapped drive", ResolutionCategory.CONFIGURATION
        );
        assertTrue(res.isSlaBreached());
        assertEquals(Boolean.FALSE, res.getResolvedWithinSla());
    }

    @Test
    void testIncidentDashboardAnalytics() {
        Incident incident = createTestIncident("INC-DASH-001");
        incidentService.acknowledge(incident.getId(), "admin");
        incidentService.resolve(incident.getId(), "admin", "Config issue", "Updated settings", ResolutionCategory.CONFIGURATION);

        IncidentDashboardResponse dash = incidentService.getIncidentDashboard();
        assertNotNull(dash);
        assertEquals(100.0, dash.getSlaCompliancePercentage());
        assertEquals(0, dash.getSlaViolations());
    }

    @Test
    void testIncidentSearch() {
        createTestIncident("INC-SEARCH-001");
        createTestIncident("INC-SEARCH-002");

        Page<IncidentSummaryResponse> page = incidentService.searchIncidents(
                AlertSeverity.HIGH,
                AlertStatus.OPEN,
                AlertSource.SYSTEM,
                null,
                null,
                "INC-SEARCH",
                null,
                null,
                PageRequest.of(0, 10)
        );
        assertEquals(2, page.getTotalElements());
    }

    private Incident createTestIncident(String number) {
        Incident incident = Incident.builder()
                .incidentNumber(number)
                .alertRule(testRule)
                .severity(AlertSeverity.HIGH)
                .source(AlertSource.SYSTEM)
                .title("Test title " + number)
                .description("Test description")
                .status(AlertStatus.OPEN)
                .firstOccurredAt(LocalDateTime.now())
                .lastOccurredAt(LocalDateTime.now())
                .occurrenceCount(1)
                .slaDeadline(LocalDateTime.now().plusHours(4))
                .slaBreached(false)
                .acknowledgementDeadline(LocalDateTime.now().plusMinutes(30))
                .escalationLevel(EscalationLevel.L1)
                .build();
        return incidentRepository.save(incident);
    }
}
