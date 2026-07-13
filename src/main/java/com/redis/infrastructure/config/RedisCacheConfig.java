package com.redis.infrastructure.config;

import com.redis.product.service.ProductService;

import com.redis.infrastructure.cache.CustomCacheErrorHandler;
import com.redis.infrastructure.cache.ResilientCacheManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "spring.cache.type",
    havingValue = "redis",
    matchIfMissing = true
)
public class RedisCacheConfig implements CachingConfigurer {

    // Cache name constants
    public static final String CACHE_PRODUCTS = "products";
    public static final String CACHE_PRODUCT  = "product";
    public static final String CACHE_USERS    = "users";

    // Cache TTL configuration constants
    private static final Duration TTL_DEFAULT  = Duration.ofMinutes(10);
    private static final Duration TTL_PRODUCTS = Duration.ofMinutes(10);
    private static final Duration TTL_PRODUCT  = Duration.ofMinutes(30);
    private static final Duration TTL_USERS    = Duration.ofMinutes(15);

    // Constructor injection for required dependencies
    private final RedisConnectionFactory connectionFactory;
    private final ObjectMapper redisObjectMapper;

    public RedisCacheConfig(
            RedisConnectionFactory connectionFactory,
            @Qualifier("redisObjectMapper") ObjectMapper redisObjectMapper) {
        this.connectionFactory  = connectionFactory;
        this.redisObjectMapper  = redisObjectMapper;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  CACHE MANAGER
    // ═══════════════════════════════════════════════════════════════════════════

    @Bean
    @Override
    @org.springframework.context.annotation.Primary
    public CacheManager cacheManager() {

        RedisCacheConfiguration defaultConfig = buildDefaultCacheConfig(TTL_DEFAULT);

        RedisCacheManager redisCacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(buildCacheConfigurations(defaultConfig))
                .build();

        return new ResilientCacheManager(redisCacheManager);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  KEY GENERATOR
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * FIX #6: Clean key format — "ProductService::findById::42"
     *         Pehle trailing underscore tha — "ProductService_findById_42_"
     */
    @Bean
    @Override
    public KeyGenerator keyGenerator() {
        return (target, method, params) -> {
            String paramsPart = (params == null || params.length == 0)
                    ? "no-params"
                    : Arrays.stream(params)
                            .map(p -> p != null ? p.toString() : "null")
                            .collect(Collectors.joining(","));

            return target.getClass().getSimpleName()
                    + "::" + method.getName()
                    + "::" + paramsPart;
        };
    }

    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new CustomCacheErrorHandler();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Private Helpers
    // ═══════════════════════════════════════════════════════════════════════════

    /** Base cache config — all caches inherit from this base configuration */
    private RedisCacheConfiguration buildDefaultCacheConfig(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new StringRedisSerializer()
                        )
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer(redisObjectMapper)
                        )
                )
                .disableCachingNullValues();
    }

    /** Map of cache-specific configurations and their respective TTL values */
    private Map<String, RedisCacheConfiguration> buildCacheConfigurations(
            RedisCacheConfiguration base) {
        return Map.of(
                CACHE_PRODUCTS, base.entryTtl(TTL_PRODUCTS),
                CACHE_PRODUCT,  base.entryTtl(TTL_PRODUCT),
                CACHE_USERS,    base.entryTtl(TTL_USERS)
        );
    }
}