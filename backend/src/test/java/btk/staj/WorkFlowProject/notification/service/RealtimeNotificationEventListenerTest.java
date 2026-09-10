package btk.staj.WorkFlowProject.notification.service;

import btk.staj.WorkFlowProject.notification.entity.Notification;
import btk.staj.WorkFlowProject.notification.entity.NotificationType;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RealtimeNotificationEventListenerTest {

    @Test
    void handlesEventsOnlyAfterSuccessfulCommit() throws NoSuchMethodException {
        TransactionalEventListener annotation = RealtimeNotificationEventListener.class
                .getMethod("handle", RealtimeNotificationCreatedEvent.class)
                .getAnnotation(TransactionalEventListener.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(annotation.fallbackExecution()).isFalse();
    }

    @Test
    void publishesNotificationWhenEventIsHandled() {
        RealtimeNotificationPublisher publisher = mock(RealtimeNotificationPublisher.class);
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        Notification notification = new Notification(
                userId, recordId, "Test notification", NotificationType.RECORD_SUBMITTED);
        RealtimeNotificationEventListener listener = new RealtimeNotificationEventListener(publisher);
        RealtimeNotificationCreatedEvent event = new RealtimeNotificationCreatedEvent(notification);

        listener.handle(event);

        verify(publisher).publish(notification);
    }
}
