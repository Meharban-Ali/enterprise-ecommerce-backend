package com.redis.infrastructure.security;

import com.redis.auth.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.servlet.HandlerExceptionResolver;
import com.redis.user.service.UserSessionService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Unit Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private UserSessionService userSessionService;

    @Mock
    private ObjectProvider<UserSessionService> userSessionServiceProvider;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HandlerExceptionResolver resolver;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ObjectProvider<RedisTemplate<String, Object>> redisTemplateProvider;

    @Mock
    private FilterChain filterChain;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        lenient().when(userSessionServiceProvider.getIfAvailable()).thenReturn(userSessionService);
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService, resolver, redisTemplateProvider, userSessionServiceProvider);
    }

    @Test
    @DisplayName("✅ Success: Should bypass filter if Authorization header is missing")
    void doFilterInternal_NoHeader_BypassesFilter() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).extractEmail(any());
    }

    @Test
    @DisplayName("✅ Success: Should bypass filter if Authorization header does not start with Bearer")
    void doFilterInternal_NonBearerHeader_BypassesFilter() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).extractEmail(any());
    }

    @Test
    @DisplayName("❌ Failure: Should block request and call resolver when token is blacklisted")
    void doFilterInternal_BlacklistedToken_BlocksRequest() throws Exception {
        String token = "blacklisted-token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractEmail(token)).thenReturn("user@example.com");
        when(redisTemplate.hasKey("blacklist::" + token)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(resolver).resolveException(eq(request), eq(response), isNull(), any(io.jsonwebtoken.security.SignatureException.class));
    }

    @Test
    @DisplayName("✅ Success: Should proceed if token is not blacklisted")
    void doFilterInternal_ValidToken_Proceeds() throws Exception {
        String token = "valid-token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractEmail(token)).thenReturn("user@example.com");
        when(redisTemplate.hasKey("blacklist::" + token)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(resolver, never()).resolveException(any(), any(), any(), any());
    }
}
