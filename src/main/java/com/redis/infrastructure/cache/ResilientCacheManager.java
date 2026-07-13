package com.redis.infrastructure.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class ResilientCacheManager implements CacheManager {

    private final CacheManager delegate;
    private final Map<String, Cache> cacheMap = new ConcurrentHashMap<>();

    public ResilientCacheManager(CacheManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public Cache getCache(String name) {
        return cacheMap.computeIfAbsent(name, k -> {
            Cache redisCache = delegate.getCache(k);
            return new ResilientCache(redisCache, new ConcurrentMapCache(k));
        });
    }

    @Override
    public Collection<String> getCacheNames() {
        return delegate.getCacheNames();
    }

    public static class ResilientCache implements Cache {
        private final Cache delegate;
        private final Cache localFallback;
        private static final Logger log = LoggerFactory.getLogger(ResilientCache.class);

        public ResilientCache(Cache delegate, Cache localFallback) {
            this.delegate = delegate;
            this.localFallback = localFallback;
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public Object getNativeCache() {
            return delegate.getNativeCache();
        }

        @Override
        public ValueWrapper get(Object key) {
            try {
                ValueWrapper val = delegate.get(key);
                if (val != null) {
                    localFallback.put(key, val.get());
                }
                return val;
            } catch (Exception e) {
                log.warn("Redis GET failed for '{}' - falling back to local cache: {}", key, e.getMessage());
                return localFallback.get(key);
            }
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            try {
                T val = delegate.get(key, type);
                if (val != null) {
                    localFallback.put(key, val);
                }
                return val;
            } catch (Exception e) {
                log.warn("Redis GET failed for '{}' - falling back to local cache: {}", key, e.getMessage());
                return localFallback.get(key, type);
            }
        }

        @Override
        public <T> T get(Object key, Callable<T> valueLoader) {
            try {
                T val = delegate.get(key, valueLoader);
                if (val != null) {
                    localFallback.put(key, val);
                }
                return val;
            } catch (Exception e) {
                log.warn("Redis GET failed for '{}' - falling back to local cache: {}", key, e.getMessage());
                return localFallback.get(key, valueLoader);
            }
        }

        @Override
        public void put(Object key, Object value) {
            try {
                delegate.put(key, value);
            } catch (Exception e) {
                log.warn("Redis PUT failed for '{}' - saving to local cache: {}", key, e.getMessage());
            }
            localFallback.put(key, value);
        }

        @Override
        public void evict(Object key) {
            try {
                delegate.evict(key);
            } catch (Exception e) {
                log.warn("Redis EVICT failed for '{}': {}", key, e.getMessage());
            }
            localFallback.evict(key);
        }

        @Override
        public void clear() {
            try {
                delegate.clear();
            } catch (Exception e) {
                log.warn("Redis CLEAR failed: {}", e.getMessage());
            }
            localFallback.clear();
        }
    }
}
