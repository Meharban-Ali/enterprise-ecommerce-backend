package com.redis.notification.dto;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public abstract class NotificationTemplateData {
    private final String companyName;
    private final String supportContact;
}
