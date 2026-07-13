package com.redis.observability.entity;

import com.redis.common.util.SensitiveDataMasker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public class StructuredLogger {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void info(String message, Map<String, Object> extraFields) {
        logJson("INFO", message, extraFields, null);
    }

    public static void warn(String message, Map<String, Object> extraFields) {
        logJson("WARN", message, extraFields, null);
    }

    public static void error(String message, Map<String, Object> extraFields, Throwable t) {
        logJson("ERROR", message, extraFields, t);
    }

    private static void logJson(String level, String message, Map<String, Object> extraFields, Throwable t) {
        Map<String, Object> logEntry = new LinkedHashMap<>();
        logEntry.put("timestamp", Instant.now().toString());
        logEntry.put("level", level);
        logEntry.put("message", SensitiveDataMasker.mask(message));
        
        String traceId = MDC.get("traceId");
        String spanId = MDC.get("spanId");
        String correlationId = MDC.get("correlationId");
        String userId = MDC.get("userId");
        String endpoint = MDC.get("endpoint");
        String module = MDC.get("module");

        if (traceId != null) logEntry.put("traceId", traceId);
        if (spanId != null) logEntry.put("spanId", spanId);
        if (correlationId != null) logEntry.put("correlationId", correlationId);
        if (userId != null) logEntry.put("userId", userId);
        if (endpoint != null) logEntry.put("endpoint", endpoint);
        if (module != null) logEntry.put("module", module);
        
        logEntry.put("thread", Thread.currentThread().getName());

        if (extraFields != null) {
            extraFields.forEach((k, v) -> {
                if (v instanceof String) {
                    logEntry.put(k, SensitiveDataMasker.mask((String) v));
                } else {
                    logEntry.put(k, v);
                }
            });
        }

        if (t != null) {
            logEntry.put("exception", t.getClass().getName());
            logEntry.put("errorMessage", SensitiveDataMasker.mask(t.getMessage()));
        }

        try {
            String json = mapper.writeValueAsString(logEntry);
            if ("INFO".equals(level)) log.info(json);
            else if ("WARN".equals(level)) log.warn(json);
            else if ("ERROR".equals(level)) log.error(json, t);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize structured log", e);
        }
    }
}
