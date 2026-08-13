package btk.staj.WorkFlowProject.notification.service;

import btk.staj.WorkFlowProject.common.dto.PagedResponse;
import btk.staj.WorkFlowProject.common.exception.ForbiddenException;
import btk.staj.WorkFlowProject.common.exception.ResourceNotFoundException;
import btk.staj.WorkFlowProject.notification.dto.NotificationResponse;
import btk.staj.WorkFlowProject.notification.entity.Notification;
import btk.staj.WorkFlowProject.notification.entity.NotificationType;
import btk.staj.WorkFlowProject.notification.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class NotificationService {

    /** Tek istekte donulebilecek en fazla bildirim sayisi. */
    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = Objects.requireNonNull(
                notificationRepository, "notificationRepository");
    }

    public Notification create(UUID userId, UUID recordId, String message, NotificationType type) {
        return notificationRepository.save(new Notification(userId, recordId, message, type));
    }

    public List<NotificationResponse> getUnread(UUID userId) {
        Objects.requireNonNull(userId, "userId");
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    /**
     * Kullanicinin tum bildirim gecmisi (okunmus + okunmamis), en yeniden
     * eskiye. Okunmamislarin aksine bu liste zamanla buyudugu icin sayfali
     * donulur (sozlesme §10).
     */
    public PagedResponse<NotificationResponse> getAll(UUID userId, Pageable pageable) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(pageable, "pageable");

        Page<NotificationResponse> page = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, sanitize(pageable))
                .map(NotificationResponse::from);

        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    /**
     * Siralama istekten alinmaz: bildirim listesi her zaman en yeniden eskiye
     * doner, boylece istemcinin gonderdigi gecersiz bir {@code sort} alani
     * sorguyu patlatamaz. Sayfa boyutu da ustten sinirlanir; aksi halde tek
     * istekle butun gecmis cekilebilirdi.
     */
    private static Pageable sanitize(Pageable pageable) {
        int size = Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE);
        return PageRequest.of(pageable.getPageNumber(), size);
    }

    public long countUnread(UUID userId) {
        Objects.requireNonNull(userId, "userId");
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * Bildirimi okundu isaretler.
     *
     * <p>Sahiplik burada dogrulanir: bildirim id'si tahmin edilebilir olmasa da
     * baskasinin bildirimini okundu isaretlemek mumkun olmamalidir.
     */
    @Transactional
    public void markAsRead(UUID notificationId, UUID currentUserId) {
        Objects.requireNonNull(notificationId, "notificationId");
        Objects.requireNonNull(currentUserId, "currentUserId");

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Bildirim bulunamadı: " + notificationId));

        if (!currentUserId.equals(notification.getUserId())) {
            throw new ForbiddenException("Bu bildirim üzerinde işlem yapma yetkiniz yok");
        }

        notification.markAsRead();
        notificationRepository.save(notification);
    }
}
