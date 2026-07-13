package com.redis.notification.service;

import com.redis.infrastructure.config.NotificationProperties;
import com.redis.notification.dto.response.NotificationPreviewResponse;
import com.redis.notification.dto.request.NotificationTemplateRequest;
import com.redis.notification.dto.response.NotificationTemplateResponse;
import com.redis.notification.entity.NotificationTemplateEntity;
import com.redis.notification.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationTemplateServiceImpl implements NotificationTemplateService {

    private final NotificationTemplateRepository templateRepository;
    private final NotificationProperties properties;
    private final CacheManager cacheManager;

    @Override
    @Transactional
    public NotificationTemplateResponse createTemplate(NotificationTemplateRequest request) {
        log.info("Creating notification template: {}", request.getTemplateCode());
        
        int version = 1;
        if (templateRepository.existsByTemplateCode(request.getTemplateCode())) {
            version = templateRepository.findMaxVersionByTemplateCode(request.getTemplateCode()) + 1;
            deactivateAllVersions(request.getTemplateCode());
        }

        NotificationTemplateEntity entity = NotificationTemplateEntity.builder()
                .templateCode(request.getTemplateCode())
                .templateName(request.getTemplateName())
                .notificationType(request.getNotificationType())
                .notificationChannel(request.getNotificationChannel())
                .subject(request.getSubject())
                .htmlTemplate(request.getHtmlTemplate())
                .textTemplate(request.getTextTemplate())
                .active(true)
                .build();
        entity.setVersion(version);

        entity = templateRepository.save(entity);
        evictCache(request.getTemplateCode());
        return mapToResponse(entity);
    }

    @Override
    @Transactional
    public NotificationTemplateResponse updateTemplate(Long id, NotificationTemplateRequest request) {
        log.info("Updating notification template ID: {}", id);
        NotificationTemplateEntity current = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template not found with ID: " + id));

        deactivateAllVersions(current.getTemplateCode());

        int nextVersion = templateRepository.findMaxVersionByTemplateCode(current.getTemplateCode()) + 1;

        NotificationTemplateEntity entity = NotificationTemplateEntity.builder()
                .templateCode(current.getTemplateCode())
                .templateName(request.getTemplateName())
                .notificationType(request.getNotificationType())
                .notificationChannel(request.getNotificationChannel())
                .subject(request.getSubject())
                .htmlTemplate(request.getHtmlTemplate())
                .textTemplate(request.getTextTemplate())
                .active(true)
                .build();
        entity.setVersion(nextVersion);

        entity = templateRepository.save(entity);
        evictCache(current.getTemplateCode());
        return mapToResponse(entity);
    }

    @Override
    @Transactional
    public NotificationTemplateResponse activateTemplate(Long id) {
        log.info("Activating notification template ID: {}", id);
        NotificationTemplateEntity entity = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template not found with ID: " + id));

        deactivateAllVersions(entity.getTemplateCode());
        entity.setActive(true);
        entity = templateRepository.save(entity);
        evictCache(entity.getTemplateCode());
        return mapToResponse(entity);
    }

    @Override
    @Transactional
    public NotificationTemplateResponse deactivateTemplate(Long id) {
        log.info("Deactivating notification template ID: {}", id);
        NotificationTemplateEntity entity = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template not found with ID: " + id));

        entity.setActive(false);
        entity = templateRepository.save(entity);
        evictCache(entity.getTemplateCode());
        return mapToResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationPreviewResponse previewTemplate(Long id, Map<String, Object> variables) {
        log.info("Generating preview for template ID: {}", id);
        NotificationTemplateEntity entity = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template not found with ID: " + id));

        org.thymeleaf.spring6.SpringTemplateEngine engine = new org.thymeleaf.spring6.SpringTemplateEngine();
        engine.setDialect(new org.thymeleaf.spring6.dialect.SpringStandardDialect());
        StringTemplateResolver resolver = new StringTemplateResolver();
        engine.setTemplateResolver(resolver);

        Context context = new Context();
        if (variables != null) {
            context.setVariables(variables);
        }

        String renderedHtml = (entity.getHtmlTemplate() != null) ? engine.process(entity.getHtmlTemplate(), context) : "";
        String renderedText = (entity.getTextTemplate() != null) ? engine.process(entity.getTextTemplate(), context) : "";
        String renderedSubject = engine.process(entity.getSubject(), context);

        return NotificationPreviewResponse.builder()
                .subject(renderedSubject)
                .renderedHtml(renderedHtml)
                .renderedText(renderedText)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationTemplateResponse> listTemplates() {
        return templateRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public NotificationTemplateResponse rollbackVersion(String templateCode, int version) {
        log.info("Rolling back template code: {} to version: {}", templateCode, version);
        NotificationTemplateEntity target = templateRepository.findByTemplateCodeAndVersion(templateCode, version)
                .orElseThrow(() -> new IllegalArgumentException("Template version not found for code: " + templateCode + ", version: " + version));

        deactivateAllVersions(templateCode);
        target.setActive(true);
        templateRepository.save(target);
        evictCache(templateCode);
        return mapToResponse(target);
    }

    private void deactivateAllVersions(String templateCode) {
        List<NotificationTemplateEntity> versions = templateRepository.findAll().stream()
                .filter(t -> t.getTemplateCode().equals(templateCode))
                .collect(Collectors.toList());
        for (NotificationTemplateEntity v : versions) {
            if (v.isActive()) {
                v.setActive(false);
                templateRepository.save(v);
            }
        }
    }

    private void evictCache(String templateCode) {
        if (properties.isTemplateCacheEnabled() && cacheManager != null) {
            Cache cache = cacheManager.getCache("notification_templates");
            if (cache != null) {
                cache.evict(templateCode);
            }
        }
    }

    private NotificationTemplateResponse mapToResponse(NotificationTemplateEntity entity) {
        return NotificationTemplateResponse.builder()
                .id(entity.getId())
                .templateCode(entity.getTemplateCode())
                .templateName(entity.getTemplateName())
                .notificationType(entity.getNotificationType())
                .notificationChannel(entity.getNotificationChannel())
                .subject(entity.getSubject())
                .htmlTemplate(entity.getHtmlTemplate())
                .textTemplate(entity.getTextTemplate())
                .version(entity.getVersion())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
