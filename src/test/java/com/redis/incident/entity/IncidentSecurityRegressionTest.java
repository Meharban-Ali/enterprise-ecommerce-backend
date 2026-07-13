package com.redis.incident.entity;

import com.redis.infrastructure.config.TestRedisConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class IncidentSecurityRegressionTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void testAlertRulesAccessDeniedForUserRole() throws Exception {
        mockMvc.perform(get("/api/admin/alerts/rules"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testAlertRulesAllowedForAdminRole() throws Exception {
        mockMvc.perform(get("/api/admin/alerts/rules"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void testIncidentDashboardAccessDeniedForUserRole() throws Exception {
        mockMvc.perform(get("/api/admin/incidents/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testIncidentDashboardAllowedForAdminRole() throws Exception {
        mockMvc.perform(get("/api/admin/incidents/dashboard"))
                .andExpect(status().isOk());
    }
}
