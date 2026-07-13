package com.redis.notification.entity;

public interface MailClient {
    void sendEmail(String to, String subject, String body, boolean isHtml);
}
