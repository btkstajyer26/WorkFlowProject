package btk.staj.WorkFlowProject.notification.controller;

import btk.staj.WorkFlowProject.common.dto.PagedResponse;
import btk.staj.WorkFlowProject.notification.dto.NotificationResponse;
import btk.staj.WorkFlowProject.notification.service.NotificationService;
import btk.staj.WorkFlowProject.workflow.port.CurrentActorProvider;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Bildirimler her zaman oturumdaki kullaniciya aittir.
 *
 * <p>Kullanici kimligi yol degiskeninden alinmaz: oyle olsaydi herkes baska bir
 * kullanicinin id'sini yazip onun bildirimlerini okuyabilirdi.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentActorProvider currentActorProvider;

    public NotificationController(NotificationService notificationService,
                                  CurrentActorProvider currentActorProvider) {
        this.notificationService = Objects.requireNonNull(notificationService, "notificationService");
        this.currentActorProvider = Objects.requireNonNull(
                currentActorProvider, "currentActorProvider");
    }

    /**
     * Oturumdaki kullanicinin butun bildirim gecmisi (okunmus + okunmamis),
     * en yeniden eskiye ve sayfali. Arayuzdeki "Tumu" gorunumu bu ucu kullanir.
     *
     * <p>Siralama istemciden alinmaz; sayfa numarasi 0'dan baslar.
     */
    @GetMapping
    public PagedResponse<NotificationResponse> getAll(Pageable pageable) {
        return notificationService.getAll(currentUserId(), pageable);
    }

    /** Oturumdaki kullanicinin okunmamis bildirimleri. */
    @GetMapping("/unread")
    public List<NotificationResponse> getUnread() {
        return notificationService.getUnread(currentUserId());
    }

    /** Okunmamis bildirim sayisi; arayuzdeki rozet icin. */
    @GetMapping("/unread/count")
    public long countUnread() {
        return notificationService.countUnread(currentUserId());
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID id) {
        notificationService.markAsRead(id, currentUserId());
        return ResponseEntity.noContent().build();
    }

    private UUID currentUserId() {
        return currentActorProvider.currentActor().id();
    }
}
