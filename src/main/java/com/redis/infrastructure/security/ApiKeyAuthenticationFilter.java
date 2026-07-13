package com.redis.infrastructure.security;

import com.redis.security.service.ApiKeyService;
import com.redis.infrastructure.config.ApiSecurityProperties;

import com.redis.security.entity.ApiKey;
import com.redis.security.repository.ApiKeyRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ApiKeyAuthenticationFilter implements Filter {

    public static final String API_KEY_HEADER = "X-API-Key";
    
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.redis.infrastructure.config.ApiSecurityProperties securityProperties;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ApiKeyRepository apiKeyRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.redis.security.service.ApiKeyService apiKeyService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private RevokedApiKeyBloomFilter bloomFilter;

    public ApiKeyAuthenticationFilter() {}

    public ApiKeyAuthenticationFilter(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    public ApiKeyAuthenticationFilter(ApiKeyRepository apiKeyRepository, com.redis.security.service.ApiKeyService apiKeyService, com.redis.infrastructure.config.ApiSecurityProperties securityProperties) {
        this.apiKeyRepository = apiKeyRepository;
        this.apiKeyService = apiKeyService;
        this.securityProperties = securityProperties;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String rawKey = httpRequest.getHeader(API_KEY_HEADER);

            if (apiKeyRepository != null && rawKey != null && !rawKey.trim().isEmpty()) {
                String hash = sha256(rawKey.trim());
                if (bloomFilter != null && bloomFilter.mightContain(hash)) {
                    log.warn("API Key might be revoked (Bloom filter hit): check database/cache.");
                }
                Optional<ApiKey> apiKeyOpt = apiKeyService != null ? apiKeyService.getApiKeyByHash(hash) : apiKeyRepository.findByKeyHashOrRotationKeyHash(hash);

                if (apiKeyOpt.isPresent()) {
                    ApiKey key = apiKeyOpt.get();
                    
                    if (key.getLockUntil() != null && key.getLockUntil().isAfter(LocalDateTime.now())) {
                        log.warn("Blocked request for locked API Key: {}", key.getName());
                        HttpServletResponse httpResponse = (HttpServletResponse) response;
                        httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        httpResponse.setContentType("application/json");
                        httpResponse.getWriter().write("{\"message\":\"API Key is locked out due to multiple failures.\",\"code\":\"API_KEY_LOCKED\"}");
                        httpResponse.getWriter().flush();
                        return;
                    }

                    boolean isValid = false;

                    if (key.isEnabled() && !key.isRevoked()) {
                        if (hash.equals(key.getKeyHash())) {
                            if (key.getExpiresAt() == null || key.getExpiresAt().isAfter(LocalDateTime.now())) {
                                isValid = true;
                            }
                        } else if (hash.equals(key.getRotationKeyHash())) {
                            if (key.getRotationExpiresAt() != null && key.getRotationExpiresAt().isAfter(LocalDateTime.now())) {
                                isValid = true;
                            }
                        }
                    }

                    if (isValid) {
                        key.setFailedAuthenticationCount(0);
                        key.setLastSuccessfulAuthentication(LocalDateTime.now());
                        apiKeyRepository.save(key);

                        List<SimpleGrantedAuthority> authorities = key.getPermissions().stream()
                                .map(p -> new SimpleGrantedAuthority("PERMISSION_" + p.name()))
                                .collect(Collectors.toList());
                        authorities.add(new SimpleGrantedAuthority("ROLE_INTEGRATION"));

                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                key.getName(), null, authorities
                        );
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        
                        org.slf4j.MDC.put("apiKeyId", String.valueOf(key.getId()));
                        log.debug("Authenticated via API Key: {}", key.getName());
                    } else {
                        log.warn("Invalid or expired API Key presented: {}", key.getName());
                        int limit = securityProperties != null ? securityProperties.getMaxFailedApiKeyAttempts() : 5;
                        int lockMinutes = securityProperties != null ? securityProperties.getApiKeyLockDurationMinutes() : 30;

                        key.setFailedAuthenticationCount(key.getFailedAuthenticationCount() + 1);
                        if (key.getFailedAuthenticationCount() >= limit) {
                            key.setLockUntil(LocalDateTime.now().plusMinutes(lockMinutes));
                            log.warn("API Key {} locked until {}", key.getName(), key.getLockUntil());
                        }
                        apiKeyRepository.save(key);
                    }
                } else {
                    log.warn("Unknown API Key presented");
                }
            }
        }

        try {
            chain.doFilter(request, response);
        } finally {
            org.slf4j.MDC.remove("apiKeyId");
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void destroy() {}
}
