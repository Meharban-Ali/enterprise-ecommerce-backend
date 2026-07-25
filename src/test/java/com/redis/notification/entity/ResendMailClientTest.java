package com.redis.notification.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ResendMailClientTest {

    private RestClient restClient;
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    private RestClient.RequestBodySpec requestBodySpec;
    private RestClient.ResponseSpec responseSpec;
    private ResendMailClient resendMailClient;

    @BeforeEach
    void setUp() {
        restClient = mock(RestClient.class);
        requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        requestBodySpec = mock(RestClient.RequestBodySpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        resendMailClient = new ResendMailClient(restClient, "re_test_key_123", "onboarding@resend.dev");
    }

    @Test
    void testSendEmailSuccess() {
        when(responseSpec.toEntity(String.class)).thenReturn(ResponseEntity.ok("{\"id\": \"msg_123\"}"));

        assertDoesNotThrow(() ->
            resendMailClient.sendEmail("customer@example.com", "Welcome", "<h1>Hello</h1>", true)
        );

        verify(restClient, times(1)).post();
        verify(requestBodyUriSpec, times(1)).uri("/emails");
    }

    @Test
    void testSendEmailFailureThrowsRuntimeException() {
        when(responseSpec.toEntity(String.class)).thenReturn(ResponseEntity.status(400).body("{\"message\": \"Invalid domain\"}"));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            resendMailClient.sendEmail("customer@example.com", "Welcome", "<h1>Hello</h1>", true)
        );

        assertTrue(exception.getMessage().contains("Resend API email dispatch failed"));
    }
}
