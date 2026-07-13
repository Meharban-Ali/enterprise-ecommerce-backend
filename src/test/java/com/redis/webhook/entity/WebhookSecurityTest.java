package com.redis.webhook.entity;

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
public class WebhookSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void testWebhookEndpointsAccessDeniedForUserRole() throws Exception {
        mockMvc.perform(get("/api/admin/webhooks"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testWebhookEndpointsAllowedForAdminRole() throws Exception {
        mockMvc.perform(get("/api/admin/webhooks"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void testWebhookStatisticsAccessDeniedForUserRole() throws Exception {
        mockMvc.perform(get("/api/admin/webhooks/statistics"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testWebhookStatisticsAllowedForAdminRole() throws Exception {
        mockMvc.perform(get("/api/admin/webhooks/statistics"))
                .andExpect(status().isOk());
    }
}
