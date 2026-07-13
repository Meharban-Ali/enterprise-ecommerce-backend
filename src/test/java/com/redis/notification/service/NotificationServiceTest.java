package com.redis.notification.service;

import com.redis.notification.dto.response.NotificationResponse;
import com.redis.notification.dto.response.NotificationSummaryResponse;
import com.redis.notification.entity.Notification;
import com.redis.user.entity.User;
import com.redis.notification.entity.NotificationChannel;
import com.redis.notification.entity.NotificationPriority;
import com.redis.notification.entity.NotificationStatus;
import com.redis.notification.entity.NotificationType;
import com.redis.notification.mapper.NotificationMapper;
import com.redis.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User owner;
    private User guest;
    private Notification ownedNotification;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).username("owner").build();
        guest = User.builder().id(2L).username("guest").build();

        ownedNotification = Notification.builder()
                .id(10L)
                .user(owner)
                .title("Test Title")
                .message("Test message content")
                .type(NotificationType.ORDER)
                .channel(NotificationChannel.EMAIL)
                .priority(NotificationPriority.MEDIUM)
                .status(NotificationStatus.SENT)
                .readStatus(false)
                .build();
    }

    @Test
    void testGetNotificationSuccess() {
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(ownedNotification));
        when(notificationMapper.toResponse(ownedNotification)).thenReturn(
                NotificationResponse.builder().id(10L).title("Test Title").build());

        NotificationResponse response = notificationService.getNotification(10L, owner);

        assertNotNull(response);
        assertEquals("Test Title", response.getTitle());
        verify(notificationRepository, times(1)).findById(10L);
    }

    @Test
    void testGetNotificationThrowsAccessDeniedForNonOwner() {
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(ownedNotification));

        assertThrows(AccessDeniedException.class, () -> notificationService.getNotification(10L, guest));
        verify(notificationRepository, times(1)).findById(10L);
        verifyNoInteractions(notificationMapper);
    }

    @Test
    void testMarkAsReadSuccess() {
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(ownedNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(ownedNotification);
        when(notificationMapper.toResponse(any(Notification.class))).thenReturn(
                NotificationResponse.builder().id(10L).readStatus(true).build());

        NotificationResponse response = notificationService.markAsRead(10L, owner);

        assertNotNull(response);
        assertTrue(response.isReadStatus());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void testDeleteNotificationSuccess() {
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(ownedNotification));

        notificationService.deleteNotification(10L, owner);

        verify(notificationRepository, times(1)).delete(ownedNotification);
    }

    @Test
    void testDeleteNotificationThrowsAccessDeniedForNonOwner() {
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(ownedNotification));

        assertThrows(AccessDeniedException.class, () -> notificationService.deleteNotification(10L, guest));
        verify(notificationRepository, never()).delete(any());
    }

    @Test
    void testGetUnreadCountSuccess() {
        when(notificationRepository.countByUserIdAndReadStatus(1L, false)).thenReturn(5L);

        long count = notificationService.getUnreadCount(owner);

        assertEquals(5L, count);
        verify(notificationRepository, times(1)).countByUserIdAndReadStatus(1L, false);
    }
}
