package com.redis.auth;

import com.redis.infrastructure.config.ApiSecurityProperties;
import com.redis.infrastructure.config.JwtProperties;
import com.redis.user.entity.User;
import com.redis.user.entity.Role;
import com.redis.auth.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import static org.junit.jupiter.api.Assertions.*;

public class JwtValidationTest {

    private JwtProperties jwtProperties;
    private ApiSecurityProperties securityProperties;
    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret("1Fgi/zPX0upMYb170+pf2TVbYGKNVoQUr3MJKqQIzgg=");
        jwtProperties.setExpirationMs(3600000);
        jwtProperties.setRefreshExpirationMs(86400000);

        securityProperties = new ApiSecurityProperties();
        securityProperties.setJwtStrictValidationEnabled(true);

        jwtService = new JwtService(jwtProperties, securityProperties);

        testUser = User.builder()
                .id(1L)
                .email("test@ecommerce.com")
                .role(Role.ROLE_USER)
                .build();
    }

    @Test
    void testStrictJwtValidationSuccess() {
        String token = jwtService.generateToken(testUser);
        assertNotNull(token);

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("test@ecommerce.com")
                .password("password")
                .authorities("ROLE_USER")
                .build();

        assertTrue(jwtService.isTokenValid(token, userDetails));
    }
}
