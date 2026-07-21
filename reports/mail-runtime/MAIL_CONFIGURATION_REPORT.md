# Mail Subsystem — Configuration Report

This report documents the active SMTP and JavaMailSender configuration properties mapped at runtime.

---

## 1. Mapped Application Properties

The following parameters are bound to the `JavaMailSenderImpl` bean during bootstrap:

| Spring Boot Property | Resolved Value (dev profile) | Log / Configuration Origin |
| :--- | :--- | :--- |
| `spring.mail.host` | `smtp.gmail.com` | Resolved from `SMTP_HOST` in `.env` |
| `spring.mail.port` | `587` | Resolved from `SMTP_PORT` in `.env` |
| `spring.mail.username` | `supportecommerces@gmail.com` | Resolved from `SMTP_USERNAME` in `.env` |
| `spring.mail.password` | `rhgxvwldmhjhdukc` | Resolved from `SMTP_PASSWORD` in `.env` |
| `spring.mail.properties.mail.smtp.auth` | `true` | Declared in `application-dev.properties` |
| `spring.mail.properties.mail.smtp.starttls.enable` | `true` | Declared in `application-dev.properties` |
| `spring.mail.properties.mail.smtp.starttls.required` | `true` | Declared in `application-dev.properties` |
| `spring.mail.properties.mail.smtp.connectiontimeout` | `5000` | Declared in `application-dev.properties` |
| `spring.mail.properties.mail.smtp.timeout` | `5000` | Declared in `application-dev.properties` |

---

## 2. Auto-Configuration Verification

* **Bean Override Check**: No custom `@Bean JavaMailSender` or `MailSender` overrides exist in the codebase.
* **Auto-Configuration Status**: Spring Boot `MailSenderAutoConfiguration` successfully resolves and registers `JavaMailSenderImpl` with the properties mapped above.
