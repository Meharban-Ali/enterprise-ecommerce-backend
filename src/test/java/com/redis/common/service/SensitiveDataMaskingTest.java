package com.redis.common.service;

import com.redis.payment.entity.Payment;

import com.redis.common.util.SensitiveDataMasker;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SensitiveDataMaskingTest {

    @Test
    void testMaskingSensitiveJson() {
        String json = "{\"username\":\"admin\", \"password\":\"secret123\", \"creditCard\":\"1234-5678-9012-3456\"}";
        String masked = SensitiveDataMasker.mask(json);
        
        assertTrue(masked.contains("\"password\":\"******\""));
        assertTrue(masked.contains("\"creditCard\":\"******\""));
        assertTrue(masked.contains("\"username\":\"admin\""));

        // Raw card masking check
        String rawLog = "Payment processing card: 1234-5678-9012-3456";
        String maskedLog = SensitiveDataMasker.mask(rawLog);
        assertTrue(maskedLog.contains("XXXX-XXXX-XXXX-XXXX"));
    }

    @Test
    void testMaskingHeadersAndForm() {
        String logMsg = "Authorization: Bearer myTokenValue, X-API-Key: ak_1234567, password=secretVal";
        String masked = SensitiveDataMasker.mask(logMsg);

        assertTrue(masked.contains("Authorization: Bearer ******"));
        assertTrue(masked.contains("X-API-Key: ******"));
        assertTrue(masked.contains("password=******"));
    }
}
