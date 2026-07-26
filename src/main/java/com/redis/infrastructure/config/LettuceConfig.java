package com.redis.infrastructure.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class LettuceConfig {

    @Bean
    public LettuceClientConfigurationBuilderCustomizer lettuceClientConfigurationCustomizer() {
        return builder -> builder
                .clientOptions(ClientOptions.builder()
                        .autoReconnect(true)
                        .pingBeforeActivateConnection(true)
                        .socketOptions(SocketOptions.builder()
                                .keepAlive(true)
                                .connectTimeout(Duration.ofSeconds(5))
                                .build())
                        .build());
    }
}
