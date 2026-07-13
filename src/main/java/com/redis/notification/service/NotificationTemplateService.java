package com.redis.notification.service;

import com.redis.notification.dto.response.NotificationPreviewResponse;
import com.redis.notification.dto.request.NotificationTemplateRequest;
import com.redis.notification.dto.response.NotificationTemplateResponse;

import java.util.List;
import java.util.Map;

public interface NotificationTemplateService {

    NotificationTemplateResponse createTemplate(NotificationTemplateRequest request);

    NotificationTemplateResponse updateTemplate(Long id, NotificationTemplateRequest request);

    NotificationTemplateResponse activateTemplate(Long id);

    NotificationTemplateResponse deactivateTemplate(Long id);

    NotificationPreviewResponse previewTemplate(Long id, Map<String, Object> variables);

    List<NotificationTemplateResponse> listTemplates();

    NotificationTemplateResponse rollbackVersion(String templateCode, int version);
}
