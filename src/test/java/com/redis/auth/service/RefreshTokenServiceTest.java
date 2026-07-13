package com.redis.auth.service;

import com.redis.infrastructure.config.JwtProperties;
import com.redis.auth.exception.TokenRefreshException;
import com.redis.auth.entity.RefreshToken;
import com.redis.user.entity.Role;
import com.redis.user.entity.User;
import com.redis.auth.repository.RefreshTokenRepository;
import com.redis.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService Unit Tests")
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User testUser;
    private final long refreshExpirationMs = 604800000L; // 7 days

    @BeforeEach
    void setUp() {
        lenient().when(jwtProperties.getRefreshExpirationMs()).thenReturn(refreshExpirationMs);

        testUser = User.builder()
                .id(123L)
                .username("john_doe")
                .email("john@example.com")
                .role(Role.ROLE_USER)
                .build();
    }

    @Nested
    @DisplayName("createRefreshToken() Tests")
    class CreateRefreshTokenTests {

        @Test
        @DisplayName("✅ Success: Should create a new refresh token and return it")
        void createRefreshToken_Success() {
            // Arrange
            when(userRepository.findById(123L)).thenReturn(Optional.of(testUser));
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            RefreshToken token = refreshTokenService.createRefreshToken(123L);

            // Assert
            assertThat(token).isNotNull();
            assertThat(token.getUser()).isEqualTo(testUser);
            assertThat(token.getToken()).isNotBlank();
            assertThat(token.getExpiryDate()).isAfter(Instant.now());

            verify(userRepository).findById(123L);
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("❌ Failure: Should throw IllegalArgumentException when user does not exist")
        void createRefreshToken_UserNotFound() {
            // Arrange
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> refreshTokenService.createRefreshToken(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User not found with ID: 999");

            verify(userRepository).findById(999L);
            verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
        }
    }

    @Nested
    @DisplayName("verifyExpiration() Tests")
    class VerifyExpirationTests {

        @Test
        @DisplayName("✅ Success: Should return token untouched when not expired")
        void verifyExpiration_NotExpired() {
            // Arrange
            RefreshToken validToken = RefreshToken.builder()
                    .token("valid-token")
                    .expiryDate(Instant.now().plusSeconds(600)) // 10 minutes in future
                    .build();

            // Act
            RefreshToken result = refreshTokenService.verifyExpiration(validToken);

            // Assert
            assertThat(result).isEqualTo(validToken);
            verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
        }

        @Test
        @DisplayName("❌ Failure: Should throw TokenRefreshException and delete token if expired")
        void verifyExpiration_Expired() {
            // Arrange
            RefreshToken expiredToken = RefreshToken.builder()
                    .token("expired-token")
                    .expiryDate(Instant.now().minusSeconds(10)) // 10 seconds in past
                    .build();

            // Act & Assert
            assertThatThrownBy(() -> refreshTokenService.verifyExpiration(expiredToken))
                    .isInstanceOf(TokenRefreshException.class)
                    .hasMessageContaining("expired-token")
                    .hasMessageContaining("Refresh token was expired");

            verify(refreshTokenRepository).delete(expiredToken);
        }
    }

    @Nested
    @DisplayName("revocation / delete Tests")
    class RevocationTests {

        @Test
        @DisplayName("✅ Success: Should call repository deleteByToken")
        void deleteByToken_Success() {
            // Act
            refreshTokenService.deleteByToken("test-token");

            // Assert
            verify(refreshTokenRepository).deleteByToken("test-token");
        }

        @Test
        @DisplayName("✅ Success: Should call repository deleteByUser if user is found")
        void deleteByUserId_Success() {
            // Arrange
            when(userRepository.findById(123L)).thenReturn(Optional.of(testUser));

            // Act
            refreshTokenService.deleteByUserId(123L);

            // Assert
            verify(userRepository).findById(123L);
            verify(refreshTokenRepository).deleteByUser(testUser);
        }

        @Test
        @DisplayName("✅ Success: Should do nothing if user is not found during deleteByUserId")
        void deleteByUserId_UserNotFound() {
            // Arrange
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // Act
            refreshTokenService.deleteByUserId(999L);

            // Assert
            verify(userRepository).findById(999L);
            verify(refreshTokenRepository, never()).deleteByUser(any(User.class));
        }
    }
}
