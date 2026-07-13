package com.redis.infrastructure.security;

import com.redis.user.entity.User;

import com.redis.infrastructure.config.PlatformReliabilityProperties;
import com.redis.audit.event.AuditEventPublisher;
import com.redis.audit.entity.AuditActionType;
import com.redis.audit.entity.AuditStatus;
import com.redis.common.entity.ResourceType;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Slf4j
@Component
public class MaintenanceModeFilter implements Filter {

    @Autowired(required = false)
    private PlatformReliabilityProperties properties;

    @Autowired(required = false)
    private AuditEventPublisher auditEventPublisher;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            if (properties != null && properties.isMaintenanceMode()) {
                String uri = httpRequest.getRequestURI();

                if (uri.contains("/health") || uri.contains("/actuator") || uri.contains("/h2-console") || uri.contains("/swagger-ui")) {
                    chain.doFilter(request, response);
                    return;
                }

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated()) {
                    boolean isAdmin = false;
                    for (GrantedAuthority authority : auth.getAuthorities()) {
                        String role = authority.getAuthority();
                        if ("ROLE_ADMIN".equals(role) || "ROLE_SUPER_ADMIN".equals(role)) {
                            isAdmin = true;
                            break;
                        }
                    }
                    if (isAdmin) {
                        chain.doFilter(request, response);
                        return;
                    }
                }

                log.info("MAINTENANCE_BLOCKED | Method={} | URI={} | User={}",
                        httpRequest.getMethod(), uri, auth != null ? auth.getName() : "anonymous");

                if (auditEventPublisher != null) {
                    auditEventPublisher.publish(
                            null, auth != null ? auth.getName() : "anonymous",
                            AuditActionType.REQUEST_REJECTED, AuditStatus.FAILED,
                            ResourceType.SYSTEM, "MAINTENANCE_MODE",
                            "Request rejected due to maintenance mode: " + uri
                    );
                }

                httpResponse.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write("{\"error\": \"Service Unavailable\", \"message\": \"System is undergoing scheduled maintenance. Please try again later.\"}");
                return;
            }
        }

        chain.doFilter(request, response);
    }
}
