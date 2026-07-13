package com.redis.security.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
public class RateLimitServiceImpl implements RateLimitService {

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    private final ConcurrentHashMap<String, Queue<Long>> inMemoryCache = new ConcurrentHashMap<>();

    @Override
    public boolean isAllowed(String key, int limit, int windowSeconds) {
        long now = System.currentTimeMillis();
        long windowStartMs = now - (windowSeconds * 1000L);

        if (redisTemplate != null) {
            try {
                String redisKey = "ratelimit:" + key;
                redisTemplate.opsForZSet().add(redisKey, String.valueOf(now), now);
                redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, windowStartMs);
                Long size = redisTemplate.opsForZSet().zCard(redisKey);
                redisTemplate.expire(redisKey, java.time.Duration.ofSeconds(windowSeconds));

                if (size != null && size > limit) {
                    log.warn("Rate limit exceeded for key: {} (count: {}, limit: {})", key, size, limit);
                    return false;
                }
                return true;
            } catch (Exception e) {
                log.error("Redis rate limit check failed, falling back to in-memory: {}", e.getMessage());
            }
        }

        Queue<Long> queue = inMemoryCache.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>());
        synchronized (queue) {
            while (!queue.isEmpty() && queue.peek() < windowStartMs) {
                queue.poll();
            }
            if (queue.size() >= limit) {
                log.warn("Rate limit exceeded (in-memory) for key: {} (count: {}, limit: {})", key, queue.size(), limit);
                return false;
            }
            queue.add(now);
            return true;
        }
    }

    @Override
    public int getRetryAfterSeconds(String key, int limit, int windowSeconds) {
        long now = System.currentTimeMillis();
        long windowStartMs = now - (windowSeconds * 1000L);

        if (redisTemplate != null) {
            try {
                String redisKey = "ratelimit:" + key;
                var range = redisTemplate.opsForZSet().rangeWithScores(redisKey, 0, 0);
                if (range != null && !range.isEmpty()) {
                    Double score = range.iterator().next().getScore();
                    if (score != null) {
                        long diffMs = (long) (score + (windowSeconds * 1000L) - now);
                        return Math.max(1, (int) (diffMs / 1000));
                    }
                }
                return windowSeconds;
            } catch (Exception e) {
                // ignore
            }
        }

        Queue<Long> queue = inMemoryCache.get(key);
        if (queue != null && !queue.isEmpty()) {
            Long oldest = queue.peek();
            if (oldest != null) {
                long diffMs = oldest + (windowSeconds * 1000L) - now;
                return Math.max(1, (int) (diffMs / 1000));
            }
        }
        return windowSeconds;
    }
}
