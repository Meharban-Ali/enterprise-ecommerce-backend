package com.redis.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Configuration properties for CORS (Cross-Origin Resource Sharing).
 * Controls which external domains/origins are permitted to make client-side REST API calls.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    /**
     * List of allowed origins. Defaults to localhost origins if not specified in configuration files.
     */
    private List<String> allowedOrigins = List.of("http://localhost:3000", "http://localhost:8080");
}
