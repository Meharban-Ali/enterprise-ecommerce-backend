package com.redis.infrastructure.security;

import com.redis.user.entity.User;
import com.redis.auth.service.JwtService;
import com.redis.user.service.UserSessionService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final HandlerExceptionResolver resolver;
    private final ObjectProvider<RedisTemplate<String, Object>> redisTemplateProvider;
    private final ObjectProvider<UserSessionService> userSessionServiceProvider;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserDetailsService userDetailsService,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver,
            ObjectProvider<RedisTemplate<String, Object>> redisTemplateProvider,
            ObjectProvider<UserSessionService> userSessionServiceProvider) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.resolver = resolver;
        this.redisTemplateProvider = redisTemplateProvider;
        this.userSessionServiceProvider = userSessionServiceProvider;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 1. If authorization header is missing or not a Bearer token, skip and delegate
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 2. Extract token and subject email
            jwt = authHeader.substring(7);
            userEmail = jwtService.extractEmail(jwt);

            // Check if token is blacklisted in Redis
            RedisTemplate<String, Object> redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate != null) {
                try {
                    if (Boolean.TRUE.equals(redisTemplate.hasKey("blacklist::" + jwt))) {
                        log.warn("Access attempt with blacklisted JWT token");
                        throw new io.jsonwebtoken.security.SignatureException("Token is blacklisted");
                    }
                } catch (io.jsonwebtoken.security.SignatureException ex) {
                    throw ex; // Re-throw the explicit blacklist signature exception
                } catch (Exception e) {
                    log.warn("Redis check failed during blacklist check for token. Proceeding with claims validation: {}", e.getMessage());
                }
            }

            // 3. If email extracted and security context has no active session, authenticate
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                // 4. Validate token authenticity
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // 5. Register authentication in SecurityContext Holder
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("User authenticated successfully via JWT: {}", userEmail);

                    // First-login password change required flow
                    if (userDetails instanceof User) {
                        User user = (User) userDetails;
                        String uri = request.getRequestURI();
                        if (user.isPasswordChangeRequired() && !uri.endsWith("/api/auth/reset-password") && !uri.endsWith("/api/auth/logout")) {
                            log.warn("Access denied for user {}: password change required", userEmail);
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"message\":\"Password change required on first login\",\"code\":\"PASSWORD_CHANGE_REQUIRED\"}");
                            response.getWriter().flush();
                            return;
                        }
                    }

                    // Update user session activity in DB and Redis
                    if (userDetails instanceof User) {
                        UserSessionService sessionService = userSessionServiceProvider.getIfAvailable();
                        if (sessionService != null) {
                            sessionService.updateSessionActivity((User) userDetails);
                        }
                    }
                }
            }
            
            // 6. Resume filter chain execution
            filterChain.doFilter(request, response);

        } catch (JwtException ex) {
            log.warn("JWT validation failed: {}", ex.getMessage());
            resolver.resolveException(request, response, null, ex);
        }
    }
}
