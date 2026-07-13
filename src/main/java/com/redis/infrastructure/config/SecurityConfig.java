package com.redis.infrastructure.config;

import com.redis.infrastructure.security.RateLimitingFilter;
import com.redis.infrastructure.security.ApiKeyAuthenticationFilter;
import com.redis.infrastructure.security.CustomAccessDeniedHandler;
import com.redis.infrastructure.security.JwtAuthenticationFilter;
import com.redis.auth.entity.CustomAuthenticationEntryPoint;
import com.redis.infrastructure.security.MaintenanceModeFilter;

import com.redis.infrastructure.config.CorsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Enables @PreAuthorize method-level security
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final ApiKeyAuthenticationFilter apiKeyAuthFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final UserDetailsService userDetailsService;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final CorsProperties corsProperties;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private MaintenanceModeFilter maintenanceModeFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 2. Disable CSRF (stateless JWT architecture)
            .csrf(AbstractHttpConfigurer::disable)

            // 3. Set request authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public auth endpoints
                .requestMatchers("/api/auth/**").permitAll()
                // Swagger UI and API docs
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()
                // H2 console (accessible in dev/local profiles only)
                .requestMatchers("/h2-console/**").permitAll()
                // Actuator health check (publicly accessible)
                .requestMatchers("/actuator/health").permitAll()
                // Webhook callbacks (externally verified via signatures)
                .requestMatchers("/api/webhooks/**").permitAll()
                // WebSocket handshakes
                .requestMatchers("/ws/notifications/**").permitAll()
                // Admin template management endpoints
                .requestMatchers("/api/admin/notification-templates/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                // User preferences endpoints
                .requestMatchers("/api/notifications/preferences/**").authenticated()
                // Admin audit and compliance framework endpoints
                .requestMatchers("/api/admin/audit/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                // Admin operational monitoring endpoints
                .requestMatchers("/api/admin/system/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                // Admin alert and incident endpoints
                .requestMatchers("/api/admin/alerts/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/admin/incidents/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/admin/webhooks/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                // Admin operational endpoints
                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                // All other requests require authentication
                .anyRequest().authenticated()
            )

            // 4. Stateless session management (no HTTP session created or used)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // 5. Custom DAO authentication provider
            .authenticationProvider(authenticationProvider())

            // 6. Custom exception handlers for 401/403 responses
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )

            // 7. Security headers
            .headers(headers -> {
                headers.frameOptions(frame -> frame.sameOrigin());
                headers.contentTypeOptions(contentType -> {});
                headers.httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                );
                headers.contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"));
                headers.referrerPolicy(referrer -> referrer.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER));
                headers.permissionsPolicy(permissions -> permissions.policy("geolocation=(), camera=(), microphone=()"));
                headers.addHeaderWriter(new org.springframework.security.web.header.writers.StaticHeadersWriter("Cross-Origin-Resource-Policy", "same-origin"));
                headers.addHeaderWriter(new org.springframework.security.web.header.writers.StaticHeadersWriter("Cross-Origin-Embedder-Policy", "require-corp"));
                headers.addHeaderWriter(new org.springframework.security.web.header.writers.StaticHeadersWriter("Cross-Origin-Opener-Policy", "same-origin"));
                headers.addHeaderWriter(new org.springframework.security.web.header.writers.StaticHeadersWriter("Cache-Control", "no-store, max-age=0"));
                headers.addHeaderWriter(new org.springframework.security.web.header.writers.StaticHeadersWriter("Pragma", "no-cache"));
                headers.addHeaderWriter(new org.springframework.security.web.header.writers.StaticHeadersWriter("Expires", "0"));
            })

            // 8. Insert Filters
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(apiKeyAuthFilter, JwtAuthenticationFilter.class)
            .addFilterAfter(rateLimitingFilter, JwtAuthenticationFilter.class);

        if (maintenanceModeFilter != null) {
            http.addFilterAfter(maintenanceModeFilter, JwtAuthenticationFilter.class);
        }

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Cache-Control", "X-Requested-With", "X-API-Key", "Idempotency-Key", "X-Correlation-ID"));
        configuration.setExposedHeaders(List.of("Authorization", "X-Correlation-ID"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  AUTHENTICATION BEANS
    // ═══════════════════════════════════════════════════════════════════════════

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // BCrypt with default strength (10 rounds)
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl hierarchy = new RoleHierarchyImpl();
        hierarchy.setHierarchy("ROLE_SUPER_ADMIN > ROLE_ADMIN\nROLE_ADMIN > ROLE_USER");
        return hierarchy;
    }

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(org.springframework.beans.factory.ObjectProvider<PermissionEvaluator> permissionEvaluatorProvider) {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        permissionEvaluatorProvider.ifAvailable(expressionHandler::setPermissionEvaluator);
        return expressionHandler;
    }
}
