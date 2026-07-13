package com.redis.monitoring.controller;

import com.redis.monitoring.entity.DatabaseHealthIndicator;
import com.redis.monitoring.entity.RedisHealthIndicator;
import com.redis.monitoring.entity.StorageHealthIndicator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthProbeController {

    @Autowired
    private DatabaseHealthIndicator databaseHealthIndicator;

    @Autowired
    private RedisHealthIndicator redisHealthIndicator;

    @Autowired
    private StorageHealthIndicator storageHealthIndicator;

    @GetMapping("/liveness")
    public ResponseEntity<Map<String, Object>> liveness() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Application is running");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/readiness")
    public ResponseEntity<Map<String, Object>> readiness() {
        Map<String, Object> response = new HashMap<>();
        boolean ready = true;

        String dbStatus = databaseHealthIndicator.checkHealth().getStatus();
        String redisStatus = redisHealthIndicator.checkHealth().getStatus();
        String storageStatus = storageHealthIndicator.checkHealth().getStatus();

        response.put("database", dbStatus);
        response.put("redis", redisStatus);
        response.put("storage", storageStatus);

        if (!"UP".equalsIgnoreCase(dbStatus) || !"UP".equalsIgnoreCase(redisStatus) || "DOWN".equalsIgnoreCase(storageStatus)) {
            ready = false;
        }

        response.put("status", ready ? "UP" : "DOWN");
        response.put("timestamp", System.currentTimeMillis());

        if (ready) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }
}
