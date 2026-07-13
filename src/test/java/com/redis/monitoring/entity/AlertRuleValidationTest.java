package com.redis.monitoring.entity;

import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.monitoring.entity.AlertRule;
import com.redis.monitoring.entity.AlertSeverity;
import com.redis.monitoring.entity.AlertSource;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class AlertRuleValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidAlertRule() {
        AlertRule rule = AlertRule.builder()
                .ruleCode("TEST_CODE")
                .ruleName("Valid Test Rule Name")
                .source(AlertSource.DATABASE)
                .severity(AlertSeverity.CRITICAL)
                .threshold(1.0)
                .enabled(true)
                .evaluationIntervalSeconds(30)
                .cooldownSeconds(60)
                .notificationEnabled(true)
                .build();

        Set<ConstraintViolation<AlertRule>> violations = validator.validate(rule);
        assertTrue(violations.isEmpty(), "Valid alert rule should not produce constraint violations");
    }

    @Test
    void testInvalidThreshold() {
        AlertRule rule = AlertRule.builder()
                .ruleCode("TEST_CODE")
                .ruleName("Invalid Threshold Rule")
                .source(AlertSource.DATABASE)
                .severity(AlertSeverity.CRITICAL)
                .threshold(-1.5) // Invalid: must be >= 0
                .enabled(true)
                .evaluationIntervalSeconds(30)
                .cooldownSeconds(60)
                .notificationEnabled(true)
                .build();

        Set<ConstraintViolation<AlertRule>> violations = validator.validate(rule);
        assertFalse(violations.isEmpty(), "Negative threshold should trigger validation constraints");
    }

    @Test
    void testInvalidEvaluationInterval() {
        AlertRule rule = AlertRule.builder()
                .ruleCode("TEST_CODE")
                .ruleName("Invalid Interval Rule")
                .source(AlertSource.DATABASE)
                .severity(AlertSeverity.CRITICAL)
                .threshold(1.0)
                .enabled(true)
                .evaluationIntervalSeconds(0) // Invalid: must be > 0
                .cooldownSeconds(60)
                .notificationEnabled(true)
                .build();

        Set<ConstraintViolation<AlertRule>> violations = validator.validate(rule);
        assertFalse(violations.isEmpty(), "Evaluation interval of 0 should trigger validation constraints");
    }
}
