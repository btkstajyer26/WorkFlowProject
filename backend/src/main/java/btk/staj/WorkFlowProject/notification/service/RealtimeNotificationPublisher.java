package btk.staj.WorkFlowProject.notification.service;

import btk.staj.WorkFlowProject.notification.dto.NotificationResponse;
import btk.staj.WorkFlowProject.notification.entity.Notification;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class RealtimeNotificationPublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    public RealtimeNotificationPublisher(SimpMessagingTemplate messagingTemplate, UserRepository userRepository) {
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
    }

    public void publish(Notification notification) {
        userRepository.findById(notification.getUserId()).ifPresent(user ->
                messagingTemplate.convertAndSendToUser(
                        user.getEmail(),
                        "/queue/notifications",
                        NotificationResponse.from(notification)));
    }
}
