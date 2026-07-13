package com.redis.common.dto;

import com.redis.notification.dto.NotificationTemplateData;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class PasswordResetTemplateData extends NotificationTemplateData {
    private final String customerName;
    private final String resetUrl;
}
