package com.redis.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class ApiPermissionTest {

    @Test
    void testApiPermissionEvaluator() {
        ApiPermissionEvaluator evaluator = new ApiPermissionEvaluator();

        // Key with ORDER_READ authority
        UsernamePasswordAuthenticationToken keyAuth = new UsernamePasswordAuthenticationToken(
                "apiKey", null, List.of(new SimpleGrantedAuthority("PERMISSION_ORDER_READ"))
        );
        assertTrue(evaluator.hasPermission(keyAuth, null, "ORDER_READ"));
        assertFalse(evaluator.hasPermission(keyAuth, null, "ORDER_WRITE"));

        // Admin override
        UsernamePasswordAuthenticationToken adminAuth = new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        assertTrue(evaluator.hasPermission(adminAuth, null, "ORDER_WRITE"));
    }
}
