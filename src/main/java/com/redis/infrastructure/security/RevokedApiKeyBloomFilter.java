package com.redis.infrastructure.security;

import com.redis.security.entity.ApiKey;
import com.redis.security.repository.ApiKeyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.BitSet;
import java.util.List;

@Component
public class RevokedApiKeyBloomFilter implements CommandLineRunner {

    private final BitSet bitSet = new BitSet(1024 * 1024);
    private final int numHashFunctions = 3;
    private final int bitSetSize = 1024 * 1024;

    @Autowired(required = false)
    private ApiKeyRepository apiKeyRepository;

    @Override
    public void run(String... args) throws Exception {
        initialize();
    }

    public synchronized void initialize() {
        bitSet.clear();
        if (apiKeyRepository == null) return;
        try {
            List<ApiKey> keys = apiKeyRepository.findAll();
            for (ApiKey key : keys) {
                if (key.isRevoked()) {
                    add(key.getKeyHash());
                    if (key.getRotationKeyHash() != null) {
                        add(key.getRotationKeyHash());
                    }
                }
            }
        } catch (Exception e) {
            // Log error silently
        }
    }

    public synchronized void add(String value) {
        if (value == null) return;
        for (int i = 0; i < numHashFunctions; i++) {
            int hash = getHash(value, i);
            bitSet.set(Math.abs(hash % bitSetSize), true);
        }
    }

    public boolean mightContain(String value) {
        if (value == null) return false;
        for (int i = 0; i < numHashFunctions; i++) {
            int hash = getHash(value, i);
            if (!bitSet.get(Math.abs(hash % bitSetSize))) {
                return false;
            }
        }
        return true;
    }

    private int getHash(String value, int index) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = (value + "_" + index).getBytes(StandardCharsets.UTF_8);
            byte[] hashBytes = digest.digest(bytes);
            int hash = 0;
            for (int i = 0; i < 4; i++) {
                hash <<= 8;
                hash |= (hashBytes[i] & 0xFF);
            }
            return hash;
        } catch (Exception e) {
            return (value + "_" + index).hashCode();
        }
    }
}
