package com.redis.monitoring.controller;

import com.redis.infrastructure.config.TestRedisConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class SystemMonitoringControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetDashboardSuccess() throws Exception {
        mockMvc.perform(get("/api/admin/system/dashboard")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.health").exists())
                .andExpect(jsonPath("$.data.metrics").exists())
                .andExpect(jsonPath("$.data.generationTimeMs").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetHealthSuccess() throws Exception {
        mockMvc.perform(get("/api/admin/system/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applicationStatus").exists())
                .andExpect(jsonPath("$.data.modules").isArray());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetJvmMetricsSuccess() throws Exception {
        mockMvc.perform(get("/api/admin/system/jvm")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.heapUsedBytes").exists())
                .andExpect(jsonPath("$.data.threadCount").exists());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testEndpointsForbiddenForGeneralUsers() throws Exception {
        mockMvc.perform(get("/api/admin/system/dashboard")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void testEndpointsUnauthorizedForAnonymous() throws Exception {
        mockMvc.perform(get("/api/admin/system/dashboard")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
