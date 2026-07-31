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

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            final String jwt = authHeader.substring(7).trim();
            if (!jwt.isEmpty() && !"null".equalsIgnoreCase(jwt) && !"undefined".equalsIgnoreCase(jwt)) {
                try {
                    // Check if token is blacklisted in Redis
                    RedisTemplate<String, Object> redisTemplate = redisTemplateProvider.getIfAvailable();
                    if (redisTemplate != null && Boolean.TRUE.equals(redisTemplate.hasKey("blacklist::" + jwt))) {
                        log.warn("Access attempt with blacklisted JWT token");
                        SecurityContextHolder.clearContext();
                        filterChain.doFilter(request, response);
                        return;
                    }

                    final String userEmail = jwtService.extractEmail(jwt);
                    if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                        if (jwtService.isTokenValid(jwt, userDetails)) {
                            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authToken);

                            UserSessionService sessionService = userSessionServiceProvider.getIfAvailable();
                            if (sessionService != null && userDetails instanceof User) {
                                sessionService.updateSessionActivity((User) userDetails);
                            }
                        }
                    }
                } catch (Exception ex) {
                    log.warn("JWT validation failed for URI {}: {} — proceeding unauthenticated", request.getRequestURI(), ex.getMessage());
                    SecurityContextHolder.clearContext();
                }
            }
        }

        filterChain.doFilter(request, response);
    }
 }
}
