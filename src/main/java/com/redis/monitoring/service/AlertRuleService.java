package com.redis.monitoring.service;

import com.redis.monitoring.dto.request.AlertRuleRequest;
import com.redis.monitoring.dto.response.AlertRuleResponse;

import java.util.List;

public interface AlertRuleService {
    List<AlertRuleResponse> getAllRules();
    AlertRuleResponse createRule(AlertRuleRequest request, String operator);
    AlertRuleResponse updateRule(Long id, AlertRuleRequest request, String operator);
    AlertRuleResponse enableRule(Long id, String operator);
    AlertRuleResponse disableRule(Long id, String operator);
}
