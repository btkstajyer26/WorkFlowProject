package btk.staj.WorkFlowProject.service;

import btk.staj.WorkFlowProject.entity.Notification;
import btk.staj.WorkFlowProject.enums.NotificationType;
import btk.staj.WorkFlowProject.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final MailService mailService;

    public NotificationService(NotificationRepository notificationRepository, MailService mailService) {
        this.notificationRepository = notificationRepository;
        this.mailService = mailService;
    }

    public Notification createNotification(UUID userId, UUID recordId, String message, NotificationType notificationType) {
    Notification notification = new Notification();
    notification.setUserId(userId);
    notification.setRecordId(recordId);
    notification.setMessage(message);
    notification.setNotificationType(notificationType); // Örn: NotificationType.WORKFLOW_STATUS_CHANGED veya SYSTEM
    notification.setRead(false);
    return notificationRepository.save(notification);
}

    @Transactional
    public void createNotificationAndSendMail(UUID targetUserId, String targetUserEmail, String targetUserName, 
                                            UUID recordId, String recordTitle, String message, 
                                            NotificationType type, String statusName) {
        
        Notification notification = new Notification(targetUserId, recordId, message, type);
        notificationRepository.save(notification);
        log.info("Uygulama içi bildirim kaydedildi. UserID: {}, RecordID: {}", targetUserId, recordId);

mailService.sendStatusChangeMail(targetUserEmail, targetUserName, recordId, recordTitle, statusName, message);    }

    public List<Notification> getUnreadNotifications(UUID userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void markAsRead(UUID notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
        });
    }
}