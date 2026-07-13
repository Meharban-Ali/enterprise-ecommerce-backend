package com.redis.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.audit.retention")
@Getter
@Setter
public class AuditRetentionProperties {
    private int retentionDays = 365;
    private boolean archiveEnabled = true;
}
