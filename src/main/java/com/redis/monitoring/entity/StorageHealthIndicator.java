package com.redis.monitoring.entity;

import com.redis.monitoring.service.HealthIndicatorService;

import com.redis.reliability.dto.ModuleHealthResponse;
import com.redis.incident.entity.PlatformIncidentHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class StorageHealthIndicator implements HealthIndicatorService {

    @Autowired
    private PlatformIncidentHelper platformIncidentHelper;

    @Override
    public String getName() {
        return "Storage";
    }

    @Override
    public ModuleHealthResponse checkHealth() {
        File path = new File(".");
        long total = path.getTotalSpace();
        long free = path.getFreeSpace();
        long usable = path.getUsableSpace();
        
        double utilization = total == 0 ? 0.0 : ((double) (total - free) / total) * 100.0;

        Map<String, Object> details = new HashMap<>();
        details.put("totalDiskBytes", total);
        details.put("freeDiskBytes", free);
        details.put("usableDiskBytes", usable);
        details.put("diskUtilizationPercent", utilization);

        String status = "UP";
        String message = "Storage utilization is healthy.";

        if (utilization >= 95.0) {
            status = "DOWN";
            message = "Critical: Disk space nearly exhausted!";
            platformIncidentHelper.triggerIncident("DISK_USAGE_HIGH", "Critical disk utilization: " + String.format("%.2f", utilization) + "%");
        } else if (utilization >= 85.0) {
            status = "WARNING";
            message = "Warning: Disk space utilization exceeds 85%";
            platformIncidentHelper.triggerIncident("DISK_USAGE_HIGH", "Warning disk utilization: " + String.format("%.2f", utilization) + "%");
        }

        return ModuleHealthResponse.builder()
                .moduleName(getName())
                .status(status)
                .message(message)
                .details(details)
                .build();
    }
}
