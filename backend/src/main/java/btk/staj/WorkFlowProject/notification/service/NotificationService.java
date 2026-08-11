package btk.staj.WorkFlowProject.notification.service;

import btk.staj.WorkFlowProject.common.exception.ForbiddenException;
import btk.staj.WorkFlowProject.common.exception.ResourceNotFoundException;
import btk.staj.WorkFlowProject.notification.dto.NotificationResponse;
import btk.staj.WorkFlowProject.notification.entity.Notification;
import btk.staj.WorkFlowProject.notification.entity.NotificationType;
import btk.staj.WorkFlowProject.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class NotificationService {

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
