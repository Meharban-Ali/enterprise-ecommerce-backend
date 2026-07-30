package com.redis.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.user.dto.request.ChangePasswordRequest;
import com.redis.user.entity.Role;
import com.redis.user.entity.User;
import com.redis.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(100L)
                .username("john.doe@example.com")
                .email("john.doe@example.com")
                .password("EncodedPassword123!")
                .role(Role.ROLE_USER)
                .accountEnabled(true)
                .accountNonLocked(true)
                .build();
    }

    @Test
    @DisplayName("✅ Success: Should change password when request is valid")
    void changePassword_Success() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("OldPassword123!")
                .newPassword("NewPassword456!")
                .confirmPassword("NewPassword456!")
                .build();

        doNothing().when(userService).changePassword(eq(100L), any(ChangePasswordRequest.class));

        mockMvc.perform(put("/api/user/change-password")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password changed successfully"));
    }

    @Test
    @DisplayName("❌ Failure: Should reject weak new password")
    void changePassword_WeakNewPassword_Rejected() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("OldPassword123!")
                .newPassword("weak")
                .confirmPassword("weak")
                .build();

        mockMvc.perform(put("/api/user/change-password")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("❌ Failure: Should return 400 when old password does not match")
    void changePassword_WrongOldPassword_Returns400() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("WrongPassword123!")
                .newPassword("NewPassword456!")
                .confirmPassword("NewPassword456!")
                .build();

        doThrow(new IllegalArgumentException("Current password does not match"))
                .when(userService).changePassword(eq(100L), any(ChangePasswordRequest.class));

        mockMvc.perform(put("/api/user/change-password")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Current password does not match"));
    }
}
