package com.redis.common.service;

import com.redis.incident.entity.PlatformIncidentHelper;

import com.redis.infrastructure.config.PlatformReliabilityProperties;
import com.redis.audit.event.AuditEventPublisher;
import com.redis.audit.entity.AuditActionType;
import com.redis.audit.entity.AuditStatus;
import com.redis.common.entity.FeatureFlag;
import com.redis.common.entity.ResourceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.EnumMap;
import java.util.Map;

@Slf4j
@Service
public class FeatureFlagServiceImpl implements FeatureFlagService {

    @Autowired(required = false)
    private PlatformReliabilityProperties properties;

    @Autowired(required = false)
    private AuditEventPublisher auditEventPublisher;

    @Autowired
    private PlatformIncidentHelper platformIncidentHelper;

    @Override
    public boolean isEnabled(FeatureFlag flag) {
        if (properties == null || !properties.isFeatureFlagsEnabled()) {
            return true;
        }
        return properties.getFeatureFlags().getOrDefault(flag.name(), true);
    }

    @Override
    public void setEnabled(FeatureFlag flag, boolean enabled, String operator) {
        if (properties == null) return;

        boolean previous = isEnabled(flag);
        if (previous == enabled) return;

        properties.getFeatureFlags().put(flag.name(), enabled);

        log.info("FEATURE_FLAG_CHANGED | Feature={} | OldValue={} | NewValue={} | Operator={}",
                flag.name(), previous, enabled, operator);

        if (auditEventPublisher != null) {
            auditEventPublisher.publish(
                    null, operator, AuditActionType.FEATURE_FLAG_CHANGED, AuditStatus.SUCCESS,
                    ResourceType.FEATURE_FLAG, flag.name(),
                    "Feature flag " + flag.name() + " changed from " + previous + " to " + enabled + " by " + operator
            );
        }

        if (!enabled && isCritical(flag)) {
            String desc = "Critical feature disabled: " + flag.name() + " (Operator: " + operator + ")";
            platformIncidentHelper.triggerIncident("FEATURE_FLAG_DISABLED", desc);
        }
    }

    @Override
    public Map<FeatureFlag, Boolean> getAllFlags() {
        Map<FeatureFlag, Boolean> map = new EnumMap<>(FeatureFlag.class);
        for (FeatureFlag flag : FeatureFlag.values()) {
            map.put(flag, isEnabled(flag));
        }
        return map;
    }

    private boolean isCritical(FeatureFlag flag) {
        return flag == FeatureFlag.AUDIT ||
               flag == FeatureFlag.SECURITY ||
               flag == FeatureFlag.RATE_LIMITING ||
               flag == FeatureFlag.API_KEYS ||
               flag == FeatureFlag.IDEMPOTENCY;
    }
}
