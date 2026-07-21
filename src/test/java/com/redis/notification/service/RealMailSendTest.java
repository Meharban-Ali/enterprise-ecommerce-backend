package com.redis.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("dev")
public class RealMailSendTest {

    @Autowired
    private JavaMailSender mailSender;

    @Test
    public void testRealMailSend() {
        assertNotNull(mailSender, "JavaMailSender should not be null");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("supportecommerces@gmail.com");
        message.setTo("supportecommerces@gmail.com");
        message.setSubject("eCommerce SMTP loopback validation");
        message.setText("This is an automated SMTP validation message sent via Google Gmail SMTP Relay.");

        System.out.println("SMTP Loopback Verification: Attempting connection to smtp.gmail.com:587...");
        mailSender.send(message);
        System.out.println("SMTP Loopback Verification: Email successfully sent and authenticated!");
    }
}
