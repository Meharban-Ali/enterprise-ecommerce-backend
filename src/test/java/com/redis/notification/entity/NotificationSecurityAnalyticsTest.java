package com.redis.notification.entity;

import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.notification.entity.MailClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
public class NotificationSecurityAnalyticsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MailClient mailClient;

    @Test
    @WithMockUser(roles = "ADMIN")
    void testSummaryEndpointAuthorizedForAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/notifications/analytics/summary"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testSummaryEndpointForbiddenForUser() throws Exception {
        mockMvc.perform(get("/api/admin/notifications/analytics/summary"))
                .andExpect(status().isForbidden());
    }
}
