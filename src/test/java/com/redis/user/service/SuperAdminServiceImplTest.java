package com.redis.user.service;

import com.redis.auth.service.RefreshTokenService;

import com.redis.auth.dto.request.RegisterRequest;
import com.redis.product.dto.response.ProductStatsResponse;
import com.redis.user.dto.response.UserResponse;
import com.redis.user.dto.response.UserSessionResponse;
import com.redis.user.entity.Role;
import com.redis.user.entity.User;
import com.redis.user.entity.UserSession;
import com.redis.product.mapper.ProductMapper;
import com.redis.product.repository.ProductRepository;
import com.redis.user.repository.UserRepository;
import com.redis.user.repository.UserSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SuperAdminServiceImpl Unit Tests")
class SuperAdminServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserSessionService userSessionService;

    @InjectMocks
    private SuperAdminServiceImpl superAdminService;

    private User testUser;
    private User testSuperAdmin;
    private UserSession testSession;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@ecommerce.com")
                .role(Role.ROLE_USER)
                .accountEnabled(true)
                .accountNonLocked(true)
                .build();

        testSuperAdmin = User.builder()
                .id(99L)
                .username("superadmin")
                .email("super@ecommerce.com")
                .role(Role.ROLE_SUPER_ADMIN)
                .accountEnabled(true)
                .accountNonLocked(true)
                .build();

        testSession = UserSession.builder()
                .id(100L)
                .userId(1L)
                .username("john_doe")
                .email("john@ecommerce.com")
                .status("ONLINE")
                .lastActivity(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("✅ Success: Should retrieve online users")
    void getOnlineUsers_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserSession> page = new PageImpl<>(List.of(testSession));
        when(userSessionRepository.findOnlineSessions(any(LocalDateTime.class), eq(pageable))).thenReturn(page);

        Page<UserSessionResponse> result = superAdminService.getOnlineUsers(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("john@ecommerce.com");
        assertThat(result.getContent().get(0).getStatus()).isEqualTo("ONLINE");
        verify(userSessionRepository).findOnlineSessions(any(LocalDateTime.class), eq(pageable));
    }

    @Test
    @DisplayName("✅ Success: Should change user account enabled status")
    void changeStatus_Enable_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserResponse result = superAdminService.changeStatus(1L, "enable");

        assertThat(result.isAccountEnabled()).isTrue();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("✅ Success: Should lock user account")
    void changeStatus_Lock_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        superAdminService.changeStatus(1L, "lock");

        verify(userRepository).save(argThat(User::isAccountEnabled));
    }

    @Test
    @DisplayName("✅ Success: Should delete non-admin user and clean up session")
    void deleteUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userSessionRepository.findByUserId(1L)).thenReturn(Optional.of(testSession));

        superAdminService.deleteUser(1L);

        verify(userRepository).delete(testUser);
        verify(userSessionRepository).delete(testSession);
    }

    @Test
    @DisplayName("❌ Failure: Should prevent deletion of Super Admin accounts")
    void deleteUser_SuperAdmin_ThrowsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.of(testSuperAdmin));

        assertThatThrownBy(() -> superAdminService.deleteUser(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Super Admin accounts cannot be deleted");

        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    @DisplayName("✅ Success: Should generate product dashboard statistics")
    void getProductStats_Success() {
        when(productRepository.count()).thenReturn(10L);
        when(productRepository.countOutOfStock()).thenReturn(2L);
        when(productRepository.countLowStock(10)).thenReturn(3L);
        when(productRepository.getAverageRating()).thenReturn(4.55);

        ProductStatsResponse stats = superAdminService.getProductStats();

        assertThat(stats.getTotalProducts()).isEqualTo(10L);
        assertThat(stats.getOutOfStockCount()).isEqualTo(2L);
        assertThat(stats.getLowStockCount()).isEqualTo(3L);
        assertThat(stats.getAverageRating()).isEqualByComparingTo("4.55");
    }

    @Test
    @DisplayName("✅ Success: Should create business admin account")
    void createAdmin_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .username("newadmin")
                .email("admin@ecommerce.com")
                .password("Password@123")
                .securityQuestion("School?")
                .securityAnswer("HighSchool")
                .build();

        when(userRepository.existsByEmail("admin@ecommerce.com")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("encoded_pass");
        when(passwordEncoder.encode("HighSchool")).thenReturn("encoded_ans");
        
        User adminUser = User.builder()
                .id(15L)
                .username("newadmin")
                .email("admin@ecommerce.com")
                .role(Role.ROLE_ADMIN)
                .accountEnabled(true)
                .accountNonLocked(true)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(adminUser);

        UserResponse response = superAdminService.createAdmin(request);

        assertThat(response.getId()).isEqualTo(15L);
        assertThat(response.getRole()).isEqualTo(Role.ROLE_ADMIN);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("✅ Success: Should reset user password directly and invalidate sessions")
    void resetUserPassword_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("NewPass@123")).thenReturn("encoded_new_pass");

        superAdminService.resetUserPassword(1L, "NewPass@123");

        assertThat(testUser.getPassword()).isEqualTo("encoded_new_pass");
        verify(userRepository).save(testUser);
        verify(refreshTokenService).deleteByUserId(1L);
        verify(userSessionService).logoutSession("john@ecommerce.com");
    }
}
