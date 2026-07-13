package com.redis.monitoring.service;

import com.redis.monitoring.dto.request.AlertRuleRequest;
import com.redis.monitoring.dto.response.AlertRuleResponse;
import com.redis.monitoring.entity.AlertRule;
import com.redis.monitoring.repository.AlertRuleRepository;
import com.redis.user.dto.response.UserResponse;
import com.redis.user.service.UserService;
import com.redis.audit.entity.AuditActionType;
import com.redis.audit.entity.AuditStatus;
import com.redis.common.entity.ResourceType;
import com.redis.audit.event.AuditEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertRuleServiceImpl implements AlertRuleService {

    private final AlertRuleRepository alertRuleRepository;
    private final UserService userService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private AuditEventPublisher auditEventPublisher;

    @Override
    public List<AlertRuleResponse> getAllRules() {
        return alertRuleRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AlertRuleResponse createRule(AlertRuleRequest request, String operator) {
        if (alertRuleRepository.findByRuleCode(request.getRuleCode()).isPresent()) {
            throw new IllegalArgumentException("Alert rule code already exists: " + request.getRuleCode());
        }

        AlertRule rule = AlertRule.builder()
                .ruleCode(request.getRuleCode())
                .ruleName(request.getRuleName())
                .source(request.getSource())
                .severity(request.getSeverity())
                .threshold(request.getThreshold())
                .enabled(request.isEnabled())
                .evaluationIntervalSeconds(request.getEvaluationIntervalSeconds())
                .cooldownSeconds(request.getCooldownSeconds())
                .notificationEnabled(request.isNotificationEnabled())
                .build();
        rule.setUpdatedBy(operator);

        rule = alertRuleRepository.save(rule);
        audit(operator, AuditActionType.ALERT_RULE_CREATED, AuditStatus.SUCCESS, String.valueOf(rule.getId()), "Created alert rule: " + rule.getRuleCode());

        return mapToResponse(rule);
    }

    @Override
    @Transactional
    public AlertRuleResponse updateRule(Long id, AlertRuleRequest request, String operator) {
        AlertRule rule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alert rule not found with ID: " + id));

        alertRuleRepository.findByRuleCode(request.getRuleCode()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new IllegalArgumentException("Alert rule code already in use: " + request.getRuleCode());
            }
        });

        rule.setRuleCode(request.getRuleCode());
        rule.setRuleName(request.getRuleName());
        rule.setSource(request.getSource());
        rule.setSeverity(request.getSeverity());
        rule.setThreshold(request.getThreshold());
        rule.setEnabled(request.isEnabled());
        rule.setEvaluationIntervalSeconds(request.getEvaluationIntervalSeconds());
        rule.setCooldownSeconds(request.getCooldownSeconds());
        rule.setNotificationEnabled(request.isNotificationEnabled());
        rule.setUpdatedBy(operator);

        rule = alertRuleRepository.save(rule);
        audit(operator, AuditActionType.ALERT_RULE_UPDATED, AuditStatus.SUCCESS, String.valueOf(rule.getId()), "Updated alert rule: " + rule.getRuleCode());

        return mapToResponse(rule);
    }

    @Override
    @Transactional
    public AlertRuleResponse enableRule(Long id, String operator) {
        AlertRule rule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alert rule not found with ID: " + id));

        rule.setEnabled(true);
        rule.setUpdatedBy(operator);
        rule = alertRuleRepository.save(rule);
        audit(operator, AuditActionType.ALERT_RULE_UPDATED, AuditStatus.SUCCESS, String.valueOf(rule.getId()), "Enabled alert rule: " + rule.getRuleCode());

        return mapToResponse(rule);
    }

    @Override
    @Transactional
    public AlertRuleResponse disableRule(Long id, String operator) {
        AlertRule rule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alert rule not found with ID: " + id));

        rule.setEnabled(false);
        rule.setUpdatedBy(operator);
        rule = alertRuleRepository.save(rule);
        audit(operator, AuditActionType.ALERT_RULE_UPDATED, AuditStatus.SUCCESS, String.valueOf(rule.getId()), "Disabled alert rule: " + rule.getRuleCode());

        return mapToResponse(rule);
    }

    private void audit(String operator, AuditActionType action, AuditStatus status, String resourceId, String desc) {
        if (auditEventPublisher == null) {
            return;
        }
        try {
            UserResponse user = userService.getUserByEmail(operator);
            Long userId = user != null ? user.getId() : null;
            auditEventPublisher.publish(userId, operator, action, status, ResourceType.ALERT_RULE, resourceId, desc);
        } catch (Exception e) {
            log.error("Failed to publish audit event for admin rule operation", e);
        }
    }

    private AlertRuleResponse mapToResponse(AlertRule rule) {
        return AlertRuleResponse.builder()
                .id(rule.getId())
                .ruleCode(rule.getRuleCode())
                .ruleName(rule.getRuleName())
                .source(rule.getSource())
                .severity(rule.getSeverity())
                .threshold(rule.getThreshold())
                .enabled(rule.isEnabled())
                .evaluationIntervalSeconds(rule.getEvaluationIntervalSeconds())
                .cooldownSeconds(rule.getCooldownSeconds())
                .notificationEnabled(rule.isNotificationEnabled())
                .version(rule.getVersion() != null ? rule.getVersion().longValue() : null)
                .updatedBy(rule.getUpdatedBy())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }
}
