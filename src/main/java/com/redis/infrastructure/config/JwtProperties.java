package com.redis.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private long expirationMs = 900000L;
    private long refreshExpirationMs = 604800000L;
}
