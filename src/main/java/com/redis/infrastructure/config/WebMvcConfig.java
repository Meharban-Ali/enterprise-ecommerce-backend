package com.redis.infrastructure.config;

import com.redis.infrastructure.governance.ApiVersionResolver;

import com.redis.security.entity.IdempotencyInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final IdempotencyInterceptor idempotencyInterceptor;
    private final com.redis.infrastructure.governance.ApiVersionResolver apiVersionResolver;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(idempotencyInterceptor)
                .excludePathPatterns("/api/webhooks/**");
        registry.addInterceptor(apiVersionResolver);
    }
}
