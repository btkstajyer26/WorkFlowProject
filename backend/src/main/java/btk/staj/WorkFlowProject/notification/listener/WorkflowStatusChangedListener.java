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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
        Set<UUID> recipients = recipientsOf(event);
        if (recipients.isEmpty()) {
            return;
        }

        String msg = message(event);
        NotificationType type = NotificationType.of(event.action());

        for (UUID recipientId : recipients) {
            notificationService.create(
                    recipientId,
                    event.recordId(),
                    msg,
                    type);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendMail(WorkflowStatusChangedEvent event) {
        Set<UUID> recipients = recipientsOf(event);
        if (recipients.isEmpty()) {
            return;
        }

        String title = recordTitle(event.recordId());
        String statusName = event.newStatus().name();

        for (UUID recipientId : recipients) {
            Optional<User> recipient = userRepository.findById(recipientId);
            if (recipient.isEmpty()) {
                log.warn("Bildirim e-postası gönderilemedi, kullanıcı bulunamadı: {}", recipientId);
                continue;
            }

            User user = recipient.get();
            mailService.sendStatusChangeMail(
                    user.getEmail(),
                    fullName(user),
                    event.recordId(),
                    title,
                    statusName,
                    event.comment());
        }
    }

    /**
     * Bildirimi kim(ler) almali:
     * <ul>
     *   <li>{@code event.assignedTo() != null} -> yalniz atanan kisi.</li>
     *   <li>{@code assignedTo == null} (nihai onay/ret) -> kaydi olusturan ve
     *       kaydi Baskana ileten yardimci ({@code Record.lastDeputyId}).</li>
     * </ul>
     * LinkedHashSet sira garantisi verir ve ayni kisi iki role denk geldiginde mukerrerligi onler.
     */
    public Set<UUID> recipientsOf(WorkflowStatusChangedEvent event) {
        Set<UUID> recipients = new LinkedHashSet<>();

        if (event.assignedTo() != null) {
            recipients.add(event.assignedTo());
            return recipients;
        }

        Optional<Record> recordOpt = recordRepository.findById(event.recordId());
        if (recordOpt.isEmpty()) {
            return Collections.emptySet();
        }

        Record record = recordOpt.get();

        if (record.getCreatedBy() != null) {
            recipients.add(record.getCreatedBy());
        }

        if (record.getLastDeputyId() != null) {
            recipients.add(record.getLastDeputyId());
        }

        return recipients;
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