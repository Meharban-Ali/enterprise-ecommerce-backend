package com.redis.common.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Slf4j
public class HmacSignatureGenerator {

    private static final String HMAC_SHA256 = "HmacSHA256";

    public static String generateSignature(String payload, String secret, long timestamp) {
        try {
            String data = timestamp + "." + payload;
            Mac sha256Mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            sha256Mac.init(secretKeySpec);
            byte[] bytes = sha256Mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(bytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to generate HMAC signature", e);
            throw new RuntimeException("HMAC computation failed", e);
        }
    }

    public static boolean verifySignature(String payload, String secret, long timestamp, String signature, long maxTimeDiffSeconds) {
        long currentTimestamp = System.currentTimeMillis() / 1000;
        if (Math.abs(currentTimestamp - timestamp) > maxTimeDiffSeconds) {
            log.warn("Webhook signature timestamp is outside acceptable bounds: diff={}", Math.abs(currentTimestamp - timestamp));
            return false;
        }

        String expectedSignature = generateSignature(payload, secret, timestamp);
        return expectedSignature.equalsIgnoreCase(signature);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
