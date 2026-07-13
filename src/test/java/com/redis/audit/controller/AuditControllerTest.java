package com.redis.audit.controller;

import com.redis.audit.service.AuditService;

import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.audit.dto.response.AuditLogResponse;
import com.redis.audit.dto.request.AuditSearchRequest;
import com.redis.audit.dto.response.AuditSummaryResponse;
import com.redis.audit.entity.AuditActionType;
import com.redis.audit.entity.AuditStatus;
import com.redis.common.entity.ResourceType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditService auditService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetSummary() throws Exception {
        AuditSummaryResponse response = AuditSummaryResponse.builder()
                .totalEvents(10)
                .successfulEvents(8)
                .failedEvents(2)
                .securityEvents(5)
                .build();

        when(auditService.getAuditSummary()).thenReturn(response);

        mockMvc.perform(get("/api/admin/audit/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalEvents").value(10))
                .andExpect(jsonPath("$.data.securityEvents").value(5));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetLogs() throws Exception {
        AuditLogResponse logVal = AuditLogResponse.builder()
                .eventId("id-1")
                .correlationId("corr-1")
                .actionType(AuditActionType.LOGIN)
                .status(AuditStatus.SUCCESS)
                .resourceType(ResourceType.USER)
                .build();

        when(auditService.searchLogs(any(AuditSearchRequest.class), any()))
                .thenReturn(new PageImpl<>(Collections.singletonList(logVal), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/admin/audit/logs")
                        .param("status", "SUCCESS")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].eventId").value("id-1"))
                .andExpect(jsonPath("$.data.content[0].correlationId").value("corr-1"));
    }
}
