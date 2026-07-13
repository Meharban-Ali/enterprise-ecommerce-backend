package com.redis.notification.controller;

import com.redis.notification.entity.Notification;

import com.redis.common.dto.ApiResponse;
import com.redis.notification.dto.response.NotificationResponse;
import com.redis.notification.dto.response.NotificationSummaryResponse;
import com.redis.user.entity.User;
import com.redis.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getMyNotifications(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("REST: Fetching notifications for user: {}", user.getUsername());
        Page<NotificationResponse> result = notificationService.getMyNotifications(user, pageable);
        return ResponseEntity.ok(ApiResponse.success("Notifications fetched successfully", result));
    }

    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getUnreadNotifications(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("REST: Fetching unread notifications for user: {}", user.getUsername());
        Page<NotificationResponse> result = notificationService.getUnreadNotifications(user, pageable);
        return ResponseEntity.ok(ApiResponse.success("Unread notifications fetched successfully", result));
    }

    @GetMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<NotificationResponse>> getNotification(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal User user) {
        log.info("REST: Fetching notification ID: {} for user: {}", notificationId, user.getUsername());
        NotificationResponse result = notificationService.getNotification(notificationId, user);
        return ResponseEntity.ok(ApiResponse.success("Notification fetched successfully", result));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<NotificationSummaryResponse>> getNotificationSummary(
            @AuthenticationPrincipal User user) {
        log.info("REST: Fetching notification summary for user: {}", user.getUsername());
        NotificationSummaryResponse result = notificationService.getNotificationSummary(user);
        return ResponseEntity.ok(ApiResponse.success("Notification summary fetched successfully", result));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal User user) {
        log.info("REST: Marking notification ID: {} as read for user: {}", notificationId, user.getUsername());
        NotificationResponse result = notificationService.markAsRead(notificationId, user);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read successfully", result));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal User user) {
        log.info("REST: Marking all notifications as read for user: {}", user.getUsername());
        notificationService.markAllAsRead(user);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read successfully"));
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal User user) {
        log.info("REST: Deleting notification ID: {} for user: {}", notificationId, user.getUsername());
        notificationService.deleteNotification(notificationId, user);
        return ResponseEntity.ok(ApiResponse.success("Notification deleted successfully"));
    }
}
