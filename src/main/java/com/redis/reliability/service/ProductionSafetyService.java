package com.redis.reliability.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ProductionSafetyService {

    private final Map<String, String> activeTokens = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> locks = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public String generateConfirmationToken(String action) {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        activeTokens.put(action, token);
        log.info("PRODUCTION_SAFETY | Generated safety token for action: {}", action);
        return token;
    }

    public boolean verifyConfirmationToken(String action, String token) {
        if (token == null) return false;
        String expected = activeTokens.remove(action);
        boolean valid = token.equals(expected);
        log.info("PRODUCTION_SAFETY | Token verification for action: {} -> {}", action, valid);
        return valid;
    }

    public void lockOperation(String action, int seconds) {
        locks.put(action, LocalDateTime.now().plusSeconds(seconds));
        log.warn("PRODUCTION_SAFETY | Operation locked: {} for {} seconds", action, seconds);
    }

    public boolean isOperationLocked(String action) {
        LocalDateTime lockUntil = locks.get(action);
        if (lockUntil == null) return false;
        if (LocalDateTime.now().isAfter(lockUntil)) {
            locks.remove(action);
            return false;
        }
        return true;
    }
}
