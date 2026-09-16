package btk.staj.WorkFlowProject.notification.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RealtimeNotificationEventListener {

    private final RealtimeNotificationPublisher publisher;

    public RealtimeNotificationEventListener(RealtimeNotificationPublisher publisher) {
        this.publisher = publisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(RealtimeNotificationCreatedEvent event) {
        publisher.publish(event.notification());
    }
}
