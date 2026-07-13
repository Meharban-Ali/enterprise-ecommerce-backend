package com.redis.notification.controller;

import com.redis.notification.dto.request.NotificationPreferenceRequest;
import com.redis.notification.dto.response.NotificationPreferenceResponse;
import com.redis.common.dto.ApiResponse;
import com.redis.user.entity.User;
import com.redis.notification.entity.NotificationChannel;
import com.redis.notification.service.UserNotificationPreferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/notifications/preferences")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationPreferenceController {

    private final UserNotificationPreferenceService preferenceService;

    @GetMapping
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> getPreferences() {
        log.info("API GET /api/notifications/preferences — retrieving preferences");
        User user = getCurrentUser();
        NotificationPreferenceResponse response = preferenceService.getPreferences(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Preferences retrieved successfully", response));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> updatePreferences(
            @Valid @RequestBody NotificationPreferenceRequest request) {
        log.info("API PUT /api/notifications/preferences — updating preferences");
        User user = getCurrentUser();
        NotificationPreferenceResponse response = preferenceService.updatePreferences(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Preferences updated successfully", response));
    }

    @PostMapping("/enable/{channel}")
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> enableChannel(
            @PathVariable("channel") String channel) {
        log.info("API POST /api/notifications/preferences/enable/{}", channel);
        User user = getCurrentUser();
        NotificationChannel nc = NotificationChannel.valueOf(channel.toUpperCase());
        NotificationPreferenceResponse response = preferenceService.enableChannel(user.getId(), nc);
        return ResponseEntity.ok(ApiResponse.success("Channel " + channel + " enabled successfully", response));
    }

    @PostMapping("/disable/{channel}")
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> disableChannel(
            @PathVariable("channel") String channel) {
        log.info("API POST /api/notifications/preferences/disable/{}", channel);
        User user = getCurrentUser();
        NotificationChannel nc = NotificationChannel.valueOf(channel.toUpperCase());
        NotificationPreferenceResponse response = preferenceService.disableChannel(user.getId(), nc);
        return ResponseEntity.ok(ApiResponse.success("Channel " + channel + " disabled successfully", response));
    }

    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> resetDefaults() {
        log.info("API POST /api/notifications/preferences/reset — resetting defaults");
        User user = getCurrentUser();
        NotificationPreferenceResponse response = preferenceService.resetDefaults(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Preferences reset to defaults successfully", response));
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User)) {
            throw new IllegalStateException("User not authenticated");
        }
        return (User) auth.getPrincipal();
    }
}
