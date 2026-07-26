package com.redis.auth.entity;

import com.redis.common.dto.ProfileUpdateRequest;
import com.redis.user.dto.response.UserResponse;
import com.redis.user.entity.Role;
import com.redis.user.entity.User;
import com.redis.user.repository.UserRepository;
import com.redis.user.service.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("User and Authentication Enhancement Unit Tests")
class UserAndAuthEnhancementTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .password("bcrypt_hashed_password")
                .role(Role.ROLE_USER)
                .build();
    }

    @Test
    @DisplayName("✅ Success: Update user profile details")
    void updateProfile_Success() {
        ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                .username("john_new")
                .email("john_new@example.com")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("john_new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserResponse response = userService.updateProfile(1L, request);

        assertThat(response.getUsername()).isEqualTo("john_new");
        assertThat(response.getEmail()).isEqualTo("john_new@example.com");
        verify(userRepository).save(testUser);
    }
}
