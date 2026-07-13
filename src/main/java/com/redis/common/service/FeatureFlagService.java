package com.redis.common.service;

import com.redis.common.entity.FeatureFlag;
import java.util.Map;

public interface FeatureFlagService {
    boolean isEnabled(FeatureFlag flag);
    void setEnabled(FeatureFlag flag, boolean enabled, String operator);
    Map<FeatureFlag, Boolean> getAllFlags();
}
