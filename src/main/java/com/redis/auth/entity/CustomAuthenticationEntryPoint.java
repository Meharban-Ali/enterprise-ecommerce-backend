package com.redis.auth.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.common.dto.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        
        log.warn("Unauthorized access attempt on URI: {} — Exception: {}", 
                request.getRequestURI(), authException.getMessage());

        // 1. Set response headers
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 Unauthorized

        // 2. Build structured error response envelope
        ApiResponse<Void> apiResponse = ApiResponse.error(
                "Unauthorized access — please provide a valid JWT access token",
                "UNAUTHORIZED_ACCESS"
        );

        // 3. Serialize and write JSON payload to response output stream
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
