package com.redis.reliability.dto;

import com.redis.monitoring.dto.MonitoringMetadata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentErrorsResponse {
    private List<RecentSystemErrorResponse> errors;
    private MonitoringMetadata metadata;
}
