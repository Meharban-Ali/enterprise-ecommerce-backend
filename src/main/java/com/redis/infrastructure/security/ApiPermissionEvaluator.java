package com.redis.infrastructure.security;

import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ApiPermissionEvaluator implements PermissionEvaluator {

    private final Map<String, Boolean> permissionCache = new ConcurrentHashMap<>();

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || permission == null) {
            return false;
        }
        
        String reqPermission = permission.toString().toUpperCase();
        String cacheKey = authentication.getName() + "::" + reqPermission;

        return permissionCache.computeIfAbsent(cacheKey, key -> {
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                String auth = authority.getAuthority();
                if ("ROLE_ADMIN".equals(auth) || "ROLE_SUPER_ADMIN".equals(auth)) {
                    return true;
                }
                if (auth.equals("PERMISSION_" + reqPermission)) {
                    return true;
                }
            }
            return false;
        });
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        return hasPermission(authentication, null, permission);
    }

    public void clearCache() {
        permissionCache.clear();
    }
}
