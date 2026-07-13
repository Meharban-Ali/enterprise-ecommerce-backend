package com.redis.notification.controller;

import com.redis.common.dto.ApiResponse;
import com.redis.notification.dto.response.NotificationPreviewResponse;
import com.redis.notification.dto.request.NotificationTemplateRequest;
import com.redis.notification.dto.response.NotificationTemplateResponse;
import com.redis.notification.service.NotificationTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/notification-templates")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class NotificationTemplateController {

    private final NotificationTemplateService templateService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationTemplateResponse>>> listTemplates() {
        log.info("API GET /api/admin/notification-templates — listing templates");
        List<NotificationTemplateResponse> templates = templateService.listTemplates();
        return ResponseEntity.ok(ApiResponse.success("Templates retrieved successfully", templates));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<NotificationTemplateResponse>> createTemplate(
            @Valid @RequestBody NotificationTemplateRequest request) {
        log.info("API POST /api/admin/notification-templates — creating template code: {}", request.getTemplateCode());
        NotificationTemplateResponse template = templateService.createTemplate(request);
        return ResponseEntity.ok(ApiResponse.success("Template created successfully", template));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<NotificationTemplateResponse>> updateTemplate(
            @PathVariable("id") Long id,
            @Valid @RequestBody NotificationTemplateRequest request) {
        log.info("API PUT /api/admin/notification-templates/{} — updating template", id);
        NotificationTemplateResponse template = templateService.updateTemplate(id, request);
        return ResponseEntity.ok(ApiResponse.success("Template updated successfully", template));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<NotificationTemplateResponse>> activateTemplate(
            @PathVariable("id") Long id) {
        log.info("API POST /api/admin/notification-templates/{}/activate", id);
        NotificationTemplateResponse template = templateService.activateTemplate(id);
        return ResponseEntity.ok(ApiResponse.success("Template activated successfully", template));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<NotificationTemplateResponse>> deactivateTemplate(
            @PathVariable("id") Long id) {
        log.info("API POST /api/admin/notification-templates/{}/deactivate", id);
        NotificationTemplateResponse template = templateService.deactivateTemplate(id);
        return ResponseEntity.ok(ApiResponse.success("Template deactivated successfully", template));
    }

    @PostMapping("/{id}/preview")
    public ResponseEntity<ApiResponse<NotificationPreviewResponse>> previewTemplate(
            @PathVariable("id") Long id,
            @RequestBody(required = false) Map<String, Object> variables) {
        log.info("API POST /api/admin/notification-templates/{}/preview", id);
        NotificationPreviewResponse preview = templateService.previewTemplate(id, variables);
        return ResponseEntity.ok(ApiResponse.success("Template preview rendered successfully", preview));
    }

    @PostMapping("/rollback")
    public ResponseEntity<ApiResponse<NotificationTemplateResponse>> rollbackTemplate(
            @RequestParam("templateCode") String templateCode,
            @RequestParam("version") int version) {
        log.info("API POST /api/admin/notification-templates/rollback — rollback code: {}, version: {}", templateCode, version);
        NotificationTemplateResponse template = templateService.rollbackVersion(templateCode, version);
        return ResponseEntity.ok(ApiResponse.success("Template rolled back successfully", template));
    }
}
