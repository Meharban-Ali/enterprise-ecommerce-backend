package com.redis.reliability.dto;

import com.redis.monitoring.dto.MonitoringMetadata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemInfoResponse {
    private String applicationName;
    private String applicationVersion;
    private String buildVersion;
    private String gitCommit;
    private String activeProfile;
    private String javaVersion;
    private String jvmVendor;
    private String operatingSystem;
    private String hostname;
    private int availableCpuCores;
    private long jvmStartTimeMs;
    private long applicationUptimeSeconds;

    // Sprint 9.1 enhancements
    private String springBootVersion;
    private String osArchitecture;
    private String timezone;
    private long jvmUptimeMs;
    private MonitoringMetadata metadata;
}
