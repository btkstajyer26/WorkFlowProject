package btk.staj.WorkFlowProject.notification.service;

import btk.staj.WorkFlowProject.notification.dto.NotificationResponse;
import btk.staj.WorkFlowProject.notification.entity.Notification;
import btk.staj.WorkFlowProject.notification.entity.NotificationType;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RealtimeNotificationPublisherTest {

    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final RealtimeNotificationPublisher publisher =
            new RealtimeNotificationPublisher(messagingTemplate, userRepository);

    @Test
    void publishesNotificationToUserDestination() {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 10, 12, 0);
        User user = mock(User.class);
        when(user.getEmail()).thenReturn("user@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        Notification notification = new Notification(
                userId, recordId, "Test notification", NotificationType.RECORD_SUBMITTED);
        ReflectionTestUtils.setField(notification, "id", notificationId);
        ReflectionTestUtils.setField(notification, "createdAt", createdAt);

        publisher.publish(notification);

        ArgumentCaptor<NotificationResponse> captor = ArgumentCaptor.forClass(NotificationResponse.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq("user@example.com"), eq("/queue/notifications"), captor.capture());
        NotificationResponse payload = captor.getValue();
        assertThat(payload.id()).isEqualTo(notificationId);
        assertThat(payload.recordId()).isEqualTo(recordId);
        assertThat(payload.message()).isEqualTo("Test notification");
        assertThat(payload.notificationType()).isEqualTo(NotificationType.RECORD_SUBMITTED);
        assertThat(payload.read()).isFalse();
        assertThat(payload.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void doesNotPublishWhenUserCannotBeResolved() {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        Notification notification = new Notification(
                userId, recordId, "Test notification", NotificationType.RECORD_SUBMITTED);

        assertDoesNotThrow(() -> publisher.publish(notification));

        verifyNoInteractions(messagingTemplate);
    }
}
