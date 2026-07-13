package com.redis.auth.service;

import com.redis.infrastructure.config.JwtProperties;
import com.redis.user.entity.Role;
import com.redis.user.entity.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;
    private JwtProperties jwtProperties;
    private User testUser;
    private final String defaultSecretKey = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret(defaultSecretKey);
        jwtProperties.setExpirationMs(900000L); // 15 mins
        jwtService = new JwtService(jwtProperties);

        testUser = User.builder()
                .id(50L)
                .username("test_user")
                .email("test@example.com")
                .role(Role.ROLE_USER)
                .build();
    }

    @Nested
    @DisplayName("Token Generation & Claims Extraction Tests")
    class TokenClaimsTests {

        @Test
        @DisplayName("✅ Success: Should generate a valid JWT and correctly extract claims")
        void generateAndExtractClaims_Success() {
            // Act
            String token = jwtService.generateToken(testUser);

            // Assert
            assertThat(token).isNotBlank();
            assertThat(jwtService.extractEmail(token)).isEqualTo("test@example.com");
            assertThat(jwtService.extractUserId(token)).isEqualTo(50L);
            assertThat(jwtService.extractRole(token)).isEqualTo("ROLE_USER");
        }
    }

    @Nested
    @DisplayName("Token Validation Tests")
    class TokenValidationTests {

        @Test
        @DisplayName("✅ Success: Should validate token when user details match and token is not expired")
        void validateToken_Success() {
            // Arrange
            String token = jwtService.generateToken(testUser);
            UserDetails userDetails = mock(UserDetails.class);
            when(userDetails.getUsername()).thenReturn("test@example.com");

            // Act
            boolean isValid = jwtService.isTokenValid(token, userDetails);

            // Assert
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("❌ Failure: Should return false when username in token does not match user details")
        void validateToken_UsernameMismatch() {
            // Arrange
            String token = jwtService.generateToken(testUser);
            UserDetails userDetails = mock(UserDetails.class);
            when(userDetails.getUsername()).thenReturn("wrong_user@example.com");

            // Act
            boolean isValid = jwtService.isTokenValid(token, userDetails);

            // Assert
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("❌ Failure: Should throw ExpiredJwtException when token is expired")
        void validateToken_ExpiredToken() {
            // Arrange: Set expiration to -5 seconds (already expired)
            jwtProperties.setExpirationMs(-5000L);
            String token = jwtService.generateToken(testUser);

            // Act & Assert
            assertThatThrownBy(() -> jwtService.extractEmail(token))
                    .isInstanceOf(ExpiredJwtException.class);
        }

        @Test
        @DisplayName("❌ Failure: Should throw SignatureException when token signature is tampered with")
        void validateToken_InvalidSignature() {
            // Arrange
            String token = jwtService.generateToken(testUser);
            String tamperedToken = token + "corrupted";

            // Act & Assert
            assertThatThrownBy(() -> jwtService.extractEmail(tamperedToken))
                    .isInstanceOf(SignatureException.class);
        }
    }

    @Nested
    @DisplayName("Secret Key Validation Tests")
    class SecretKeyValidationTests {

        @Test
        @DisplayName("❌ Failure: Should throw IllegalStateException if JWT secret is too short")
        void validateSecretKey_TooShort() {
            // Arrange
            jwtProperties.setSecret("dGhpcy1pcy1hLXdlay1rZXk="); // weak base64 string
            jwtService = new JwtService(jwtProperties);

            // Act & Assert
            assertThatThrownBy(() -> jwtService.validateSecretKey())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("JWT secret key must be at least 256 bits");
        }

        @Test
        @DisplayName("❌ Failure: Should throw IllegalStateException if JWT secret is not valid Base64")
        void validateSecretKey_InvalidBase64() {
            // Arrange
            jwtProperties.setSecret("not-base64!!!");
            jwtService = new JwtService(jwtProperties);

            // Act & Assert
            assertThatThrownBy(() -> jwtService.validateSecretKey())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("JWT secret key must be a valid Base64 encoded string");
        }

        @Test
        @DisplayName("✅ Success: Should pass validation when JWT secret is strong and valid Base64")
        void validateSecretKey_Success() {
            // Arrange
            jwtProperties.setSecret("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
            jwtService = new JwtService(jwtProperties);

            // Act & Assert (no exception)
            jwtService.validateSecretKey();
        }
    }
}
