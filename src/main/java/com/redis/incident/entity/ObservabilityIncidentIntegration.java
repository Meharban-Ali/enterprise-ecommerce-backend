package com.redis.incident.entity;

import com.redis.observability.entity.TraceContext;
import com.redis.observability.entity.TraceContextHolder;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Map;

@Slf4j
@Component
public class ObservabilityIncidentIntegration {

    public void triggerIncident(String incidentType, String severity, String description, Map<String, Object> contextData) {
        // Integrate with existing Incident Framework
        // e.g., incidentService.createIncident(...)
        
        TraceContext ctx = TraceContextHolder.getContext();
        String traceId = ctx != null ? ctx.getTraceId() : "N/A";
        
        log.error("AUTOMATIC_INCIDENT | Type={} | Severity={} | TraceId={} | Description={}", 
            incidentType, severity, traceId, description);
            
        if (contextData != null && !contextData.isEmpty()) {
            log.error("Incident Context: {}", contextData);
        }
    }
}
