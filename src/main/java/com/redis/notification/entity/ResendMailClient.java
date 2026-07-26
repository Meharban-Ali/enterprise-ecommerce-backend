package com.redis.notification.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "resend")
public class ResendMailClient implements MailClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String fromEmail;

    @org.springframework.beans.factory.annotation.Autowired
    public ResendMailClient(
            RestClient.Builder restClientBuilder,
            @Value("${app.resend.api-key}") String apiKey,
            @Value("${app.resend.from-email:onboarding@resend.dev}") String fromEmail) {
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
        this.restClient = restClientBuilder
                .baseUrl("https://api.resend.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // Constructor for testing with pre-configured RestClient
    public ResendMailClient(RestClient restClient, String apiKey, String fromEmail) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
    }

    @Override
    public void sendEmail(String to, String subject, String body, boolean isHtml) {
        log.info("Attempting to send email via Resend API to: {} with subject: {}", to, subject);

        ResendEmailRequest requestPayload = ResendEmailRequest.builder()
                .from(fromEmail)
                .to(List.of(to))
                .subject(subject)
                .html(isHtml ? body : null)
                .text(isHtml ? null : body)
                .build();

        try {
            ResponseEntity<String> response = restClient.post()
                    .uri("/emails")
                    .body(requestPayload)
                    .retrieve()
                    .toEntity(String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Resend API success: to={}, subject={}, response={}", to, subject, response.getBody());
            } else {
                log.error("Resend API non-2xx status: to={}, status={}, body={}", to, response.getStatusCode(), response.getBody());
                throw new RuntimeException("Resend API email dispatch failed with status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Resend API error: to={}, error={}", to, e.getMessage(), e);
            throw new RuntimeException("Resend API email dispatch failed: " + e.getMessage(), e);
        }
    }

    @Getter
    @Builder
    public static class ResendEmailRequest {
        private String from;
        private List<String> to;
        private String subject;
        private String html;
        private String text;
    }
}
