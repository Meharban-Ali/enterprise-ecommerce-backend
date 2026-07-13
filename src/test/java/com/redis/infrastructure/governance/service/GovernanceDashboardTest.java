package com.redis.infrastructure.governance.service;

import com.redis.infrastructure.governance.dto.ApiGovernanceDashboardResponse;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class GovernanceDashboardTest {

    @Test
    void testGovernanceDashboardMetrics() {
        ApiGovernanceService service = new ApiGovernanceServiceImpl();

        service.recordRequest("/api/v1/products", "GET", 50L, 200, 100, "user1", false, false, false, false);
        service.recordRequest("/api/v1/products", "GET", 150L, 200, 100, "user1", false, false, false, false);
        service.recordRequest("/api/v1/checkout", "POST", 500L, 500, 200, "key:admin-key", false, false, false, true);

        ApiGovernanceDashboardResponse dash = service.getDashboard();
        
        assertNotNull(dash);
        assertEquals(3L, dash.getTotalRequests());
        assertEquals(66.67, dash.getSuccessRate(), 0.1);
        assertEquals(1L, dash.getValidationFailures());
        assertEquals(233.33, dash.getAverageResponseTimeMs(), 0.1);
        assertTrue(dash.getMostCalledApis().containsKey("GET /api/v1/products"));
        assertEquals(2L, dash.getMostCalledApis().get("GET /api/v1/products"));
        assertTrue(dash.getApiKeyUsage().containsKey("admin-key"));
        assertEquals(1L, dash.getApiKeyUsage().get("admin-key"));
    }
}
