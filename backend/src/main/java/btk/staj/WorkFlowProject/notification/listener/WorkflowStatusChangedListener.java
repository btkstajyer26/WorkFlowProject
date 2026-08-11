package btk.staj.WorkFlowProject.notification.listener;

import btk.staj.WorkFlowProject.notification.entity.NotificationType;
import btk.staj.WorkFlowProject.notification.service.MailService;
import btk.staj.WorkFlowProject.notification.service.NotificationService;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import btk.staj.WorkFlowProject.workflow.model.WorkflowStatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Onay akisi bir durum degisikligi yayinladiginda bildirim uretir.
 *
 * <p>Iki kanal bilerek farkli anlarda calisir:
 *
 * <ul>
 *   <li><strong>Uygulama ici bildirim</strong> gecisle ayni transaction'da
 *       yazilir. Sozlesme (bkz. {@code docs/FRONTEND_BACKEND_SOZLESMESI.md})
 *       durum guncellemesi, denetim izi ve bildirimin tek transaction'da
 *       tamamlanmasini soyluyor; gecis geri alinirsa bildirim de kalmaz.</li>
 *   <li><strong>E-posta</strong> yalnizca commit sonrasi gonderilir. Geri
 *       alinabilir bir islem icin disariya e-posta cikmasi geri alinamaz.</li>
 * </ul>
 */
@Component
public class WorkflowStatusChangedListener {

    private static final Logger log = LoggerFactory.getLogger(WorkflowStatusChangedListener.class);

    private final NotificationService notificationService;
    private final MailService mailService;
    private final RecordRepository recordRepository;
    private final UserRepository userRepository;

    public WorkflowStatusChangedListener(NotificationService notificationService,
                                         MailService mailService,
                                         RecordRepository recordRepository,
                                         UserRepository userRepository) {
        this.notificationService = Objects.requireNonNull(notificationService, "notificationService");
        this.mailService = Objects.requireNonNull(mailService, "mailService");
        this.recordRepository = Objects.requireNonNull(recordRepository, "recordRepository");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
    }

    @EventListener
    public void createInAppNotification(WorkflowStatusChangedEvent event) {
        UUID recipientId = recipientOf(event);
        if (recipientId == null) {
            return;
        }

        notificationService.create(
                recipientId,
                event.recordId(),
                message(event),
                NotificationType.of(event.action()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendMail(WorkflowStatusChangedEvent event) {
        UUID recipientId = recipientOf(event);
        if (recipientId == null) {
            return;
        }

        Optional<User> recipient = userRepository.findById(recipientId);
        if (recipient.isEmpty()) {
            log.warn("Bildirim e-postası gönderilemedi, kullanıcı bulunamadı: {}", recipientId);
            return;
        }

        User user = recipient.get();
        mailService.sendStatusChangeMail(
                user.getEmail(),
                fullName(user),
                event.recordId(),
                recordTitle(event.recordId()),
                event.newStatus().name(),
                event.comment());
    }

    /**
     * Bildirimi kim almali: gecis sonrasi sirasi gelen kisi. Onay ve reddin
     * ardindan kayit kimseye atanmaz; o zaman haberi olmasi gereken kisi
     * evragi olusturandir.
     */
    private UUID recipientOf(WorkflowStatusChangedEvent event) {
        if (event.assignedTo() != null) {
            return event.assignedTo();
        }
        return recordRepository.findById(event.recordId())
                .map(Record::getCreatedBy)
                .orElse(null);
    }

    private String recordTitle(UUID recordId) {
        return recordRepository.findById(recordId)
                .map(Record::getTitle)
                .orElse("(başlık okunamadı)");
    }

    private static String fullName(User user) {
        return (user.getFirstName() + " " + user.getLastName()).trim();
    }

    private static String message(WorkflowStatusChangedEvent event) {
        String base = switch (NotificationType.of(event.action())) {
            case RECORD_SUBMITTED -> "Bir evrak incelemenize sunuldu";
            case RECORD_FORWARDED -> "Bir evrak onayınıza iletildi";
            case RECORD_APPROVED -> "Evrağınız onaylandı";
            case RECORD_REJECTED -> "Evrağınız reddedildi";
            case RECORD_RETURNED -> "Evrağınız düzeltme için geri gönderildi";
        };

        if (event.comment() == null || event.comment().isBlank()) {
            return base;
        }
        return truncate(base + ": " + event.comment());
    }

    /** message kolonu VARCHAR(500); uzun aciklama yazmayi engellememeli. */
    private static String truncate(String value) {
        return value.length() <= 500 ? value : value.substring(0, 497) + "...";
    }
}
