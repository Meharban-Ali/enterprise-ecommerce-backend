package com.redis.observability.dto.response;

import com.redis.infrastructure.governance.dto.ApiGovernanceDashboardResponse;
import com.redis.infrastructure.governance.service.ApiGovernanceServiceImpl;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ResponseSizeMetricsTest {

    @Test
    void testResponseSizeRecordedAndAggregated() {
        ApiGovernanceServiceImpl service = new ApiGovernanceServiceImpl();

        service.recordRequest(
                "/api/products", "GET", 120, 200, 500, 1500,
                "user1", false, false, false, false
        );

        ApiGovernanceDashboardResponse dashboard = service.getDashboard();

        assertEquals(1, dashboard.getTotalRequests());
        assertEquals(1500.0, dashboard.getAverageResponseSizeBytes());
        assertEquals(1500, dashboard.getMaxResponseSizeBytes());
    }
}
