package com.redis.security.entity;

import com.redis.infrastructure.security.MultiReadHttpServletRequestWrapper;

import com.redis.infrastructure.config.ApiSecurityProperties;
import com.redis.infrastructure.governance.service.ApiGovernanceService;
import com.redis.security.service.IdempotencyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingResponseWrapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Slf4j
@Component
public class IdempotencyInterceptor implements HandlerInterceptor {

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ApiSecurityProperties securityProperties;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private IdempotencyService idempotencyService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ApiGovernanceService governanceService;

    public IdempotencyInterceptor() {
        this.securityProperties = new ApiSecurityProperties();
    }

    public IdempotencyInterceptor(ApiSecurityProperties securityProperties) {
        this.securityProperties = securityProperties != null ? securityProperties : new ApiSecurityProperties();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (idempotencyService == null || !securityProperties.isIdempotencyEnabled() || !"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String key = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        if (key == null || key.trim().isEmpty()) {
            return true;
        }

        String fingerprint = generateFingerprint(request);
        
        try {
            IdempotencyService.IdempotentResponse cached = idempotencyService.getCachedResponse(key, fingerprint);
            if (cached != null) {
                log.info("Idempotency hit for key: {}", key);
                
                response.setStatus(cached.getStatus());
                response.setContentType("application/json");
                cached.getHeaders().forEach(response::setHeader);
                response.getWriter().write(cached.getBody());
                response.getWriter().flush();

                recordGovernanceMetric(request, cached.getStatus(), key, true, false, false);
                return false;
            }
        } catch (IllegalArgumentException e) {
            log.error("Replay attack or fingerprint mismatch detected for key: {}", key);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"" + e.getMessage() + "\",\"code\":\"REPLAY_ATTACK_DETECTED\"}");
            response.getWriter().flush();
            
            recordGovernanceMetric(request, HttpServletResponse.SC_BAD_REQUEST, key, false, true, false);
            return false;
        }

        boolean locked = idempotencyService.acquireLock(key, fingerprint, securityProperties.getIdempotencyTtlHours());
        if (!locked) {
            log.warn("Concurrent duplicate request detected for key: {}", key);
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Duplicate request in progress\",\"code\":\"CONCURRENT_REQUEST\"}");
            response.getWriter().flush();
            return false;
        }

        request.setAttribute("idempotencyKey", key);
        request.setAttribute("idempotencyFingerprint", fingerprint);
        org.slf4j.MDC.put("idempotencyKey", key);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        String key = (String) request.getAttribute("idempotencyKey");
        String fingerprint = (String) request.getAttribute("idempotencyFingerprint");

        if (idempotencyService != null && key != null && fingerprint != null) {
            org.slf4j.MDC.remove("idempotencyKey");
            
            int status = response.getStatus();
            String body = "";
            Map<String, String> headers = new HashMap<>();

            if (response instanceof ContentCachingResponseWrapper) {
                ContentCachingResponseWrapper wrapper = (ContentCachingResponseWrapper) response;
                body = new String(wrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
                for (String headerName : wrapper.getHeaderNames()) {
                    headers.put(headerName, wrapper.getHeader(headerName));
                }
            }

            if (status >= 200 && status < 300) {
                idempotencyService.completeProcessing(key, fingerprint, status, body, headers);
                recordGovernanceMetric(request, status, key, false, false, false);
            } else {
                idempotencyService.completeProcessing(key, fingerprint, status, "", Collections.emptyMap());
                recordGovernanceMetric(request, status, key, false, false, false);
            }
        }
    }

    private void recordGovernanceMetric(HttpServletRequest request, int status, String key, boolean isHit, boolean isReplay, boolean isRateLimit) {
        if (isHit) request.setAttribute("idempotencyHit", true);
        if (isReplay) request.setAttribute("idempotencyReplay", true);
        String apiKey = request.getHeader("X-API-Key");
        String consumer = apiKey != null ? "key:" + apiKey : request.getRemoteAddr();
        request.setAttribute("consumerKey", consumer);
    }

    public String generateFingerprint(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString() != null ? request.getQueryString() : "";
        String user = SecurityContextHolder.getContext().getAuthentication() != null ? 
                SecurityContextHolder.getContext().getAuthentication().getName() : "anonymous";
        String ip = request.getRemoteAddr();

        String bodyHash = "";
        if (request instanceof MultiReadHttpServletRequestWrapper) {
            MultiReadHttpServletRequestWrapper wrapped = (MultiReadHttpServletRequestWrapper) request;
            bodyHash = sha256(new String(wrapped.getBody(), StandardCharsets.UTF_8));
        }

        return sha256(method + "|" + uri + "|" + query + "|" + user + "|" + ip + "|" + bodyHash);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 hashing missing", e);
        }
    }
}
