package com.redis.infrastructure.governance;

import com.redis.infrastructure.config.ApiSecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ApiVersionResolver implements HandlerInterceptor {

    @Autowired(required = false)
    private ApiSecurityProperties securityProperties = new ApiSecurityProperties();

    public ApiVersionResolver() {}

    public ApiVersionResolver(ApiSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        boolean isEnabled = securityProperties == null || securityProperties.isApiVersioningEnabled();
        if (!isEnabled) {
            return true;
        }

        String version = (String) request.getAttribute("resolvedApiVersion");
        if (version != null) {
            ApiDeprecationPolicy.DeprecationDetail policy = ApiDeprecationPolicy.getPolicy(version);
            if (policy != null) {
                response.setHeader("Deprecation", policy.getDeprecationHeaderValue());
                String sunset = policy.getSunsetHeaderValue();
                if (sunset != null && !sunset.isEmpty()) {
                    response.setHeader("Sunset", sunset);
                }
                String link = policy.getLinkHeaderValue();
                if (link != null && !link.isEmpty()) {
                    response.setHeader("Link", link);
                }
            }
            response.setHeader("X-API-Version", version);
        }
        return true;
    }
}
