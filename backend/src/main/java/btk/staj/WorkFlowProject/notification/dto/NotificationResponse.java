package btk.staj.WorkFlowProject.notification.dto;

import btk.staj.WorkFlowProject.notification.entity.Notification;
import btk.staj.WorkFlowProject.notification.entity.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

/** Bildirimin API'de donen bicimi; entity dogrudan donulmez. */
public record NotificationResponse(
        UUID id,
        UUID recordId,
        String message,
        NotificationType notificationType,
        boolean read,
        LocalDateTime createdAt) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getRecordId(),
                notification.getMessage(),
                notification.getNotificationType(),
                notification.isRead(),
                notification.getCreatedAt());
    }
}
