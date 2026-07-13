package com.redis.notification.controller;

import com.redis.notification.entity.Notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.notification.dto.response.NotificationResponse;
import com.redis.notification.dto.response.NotificationSummaryResponse;
import com.redis.user.entity.User;
import com.redis.user.entity.Role;
import com.redis.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("user@example.com")
                .role(Role.ROLE_USER)
                .accountEnabled(true)
                .accountNonLocked(true)
                .build();
    }

    @Test
    void testGetMyNotificationsSuccess() throws Exception {
        NotificationResponse res = NotificationResponse.builder()
                .id(10L)
                .title("Welcome Title")
                .message("Welcome Message")
                .readStatus(false)
                .build();
        Page<NotificationResponse> page = new PageImpl<>(List.of(res), PageRequest.of(0, 20), 1);

        when(notificationService.getMyNotifications(any(User.class), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/notifications/my")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].title").value("Welcome Title"));
    }

    @Test
    void testGetUnreadNotificationsSuccess() throws Exception {
        NotificationResponse res = NotificationResponse.builder()
                .id(11L)
                .title("Unread Alert")
                .readStatus(false)
                .build();
        Page<NotificationResponse> page = new PageImpl<>(List.of(res), PageRequest.of(0, 20), 1);

        when(notificationService.getUnreadNotifications(any(User.class), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/notifications/unread")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].title").value("Unread Alert"));
    }

    @Test
    void testGetNotificationByIdSuccess() throws Exception {
        NotificationResponse res = NotificationResponse.builder()
                .id(10L)
                .title("Target Notification")
                .build();

        when(notificationService.getNotification(eq(10L), any(User.class))).thenReturn(res);

        mockMvc.perform(get("/api/notifications/10")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Target Notification"));
    }

    @Test
    void testGetSummarySuccess() throws Exception {
        NotificationSummaryResponse summary = NotificationSummaryResponse.builder()
                .unreadCount(5L)
                .lastNotificationTime(LocalDateTime.now())
                .recentNotifications(List.of(NotificationResponse.builder().id(10L).title("Recent").build()))
                .build();

        when(notificationService.getNotificationSummary(any(User.class))).thenReturn(summary);

        mockMvc.perform(get("/api/notifications/summary")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.unreadCount").value(5));
    }

    @Test
    void testMarkAsReadSuccess() throws Exception {
        NotificationResponse res = NotificationResponse.builder()
                .id(10L)
                .readStatus(true)
                .build();

        when(notificationService.markAsRead(eq(10L), any(User.class))).thenReturn(res);

        mockMvc.perform(patch("/api/notifications/10/read")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.readStatus").value(true));
    }

    @Test
    void testMarkAllAsReadSuccess() throws Exception {
        doNothing().when(notificationService).markAllAsRead(any(User.class));

        mockMvc.perform(patch("/api/notifications/read-all")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testDeleteNotificationSuccess() throws Exception {
        doNothing().when(notificationService).deleteNotification(eq(10L), any(User.class));

        mockMvc.perform(delete("/api/notifications/10")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testGetMyNotificationsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/notifications/my")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
