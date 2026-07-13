package com.redis.notification.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreviewResponse {
    private String subject;
    private String renderedHtml;
    private String renderedText;
}
