package btk.staj.WorkFlowProject.listener; // veya mevcut listener paketin

import btk.staj.WorkFlowProject.event.WorkflowStatusChangedEvent; // 👈 Event'i buradan çağırıyoruz
import btk.staj.WorkFlowProject.service.MailService;
import btk.staj.WorkFlowProject.service.NotificationService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import btk.staj.WorkFlowProject.enums.NotificationType;
import java.util.UUID;

@Component
public class WorkflowStatusChangedListener {

    private final MailService mailService;
    private final NotificationService notificationService;

    public WorkflowStatusChangedListener(MailService mailService, NotificationService notificationService) {
        this.mailService = mailService;
        this.notificationService = notificationService;
    }

    @EventListener
public void handleWorkflowStatusChanged(WorkflowStatusChangedEvent event) {
    // 1. Veritabanına uygulama içi bildirim kaydı
    // NOT: İlk parametre 'userId' beklediği için şimdilik event.getRecordId() verdik.
    notificationService.createNotification(
        event.getRecordId(), 
        event.getRecordId(), 
        "Evrak Durumu Değişti: " + event.getStatus(),
        NotificationType.RECORD_APPROVED
    );

    // 2. Asenkron e-posta gönderimi
    mailService.sendStatusChangeMail(
        event.getUserEmail(),
        event.getRecipientName(),
        event.getRecordId(),
        event.getTitle(),
        event.getStatus(),
        event.getReason()
    );
}
}