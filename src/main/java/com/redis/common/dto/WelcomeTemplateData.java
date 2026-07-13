package com.redis.common.dto;

import com.redis.notification.dto.NotificationTemplateData;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class WelcomeTemplateData extends NotificationTemplateData {
    private final String customerName;
}
