package btk.staj.WorkFlowProject.notification.service;

import btk.staj.WorkFlowProject.notification.entity.DeviceToken;
import btk.staj.WorkFlowProject.notification.entity.NotificationType;
import btk.staj.WorkFlowProject.notification.repository.DeviceTokenRepository;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private final DeviceTokenRepository deviceTokenRepository;

    @Value("${fcm.project-id:}")
    private String fcmProjectId;

    @Value("${fcm.client-email:}")
    private String fcmClientEmail;

    @Value("${fcm.private-key:}")
    private String fcmPrivateKey;

    @PostConstruct
    public void init() {
        if (FirebaseApp.getApps().isEmpty() && fcmProjectId != null && !fcmProjectId.isBlank()) {
            try {
                String privateKeyClean = fcmPrivateKey.replace("\\n", "\n");
                String serviceAccountJson = String.format("""
                        {
                          "type": "service_account",
                          "project_id": "%s",
                          "client_email": "%s",
                          "private_key": "%s"
                        }
                        """, fcmProjectId, fcmClientEmail, privateKeyClean);

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(
                                new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8))))
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("FCM FirebaseApp başarıyla başlatıldı.");
            } catch (Exception e) {
                log.warn("FCM FirebaseApp başlatılamadı (env bilgileri eksik veya geçersiz olabilir): {}", e.getMessage());
            }
        }
    }

    @Async
    public void sendPushNotification(UUID userId, String title, String body, UUID recordId, NotificationType type) {
        List<DeviceToken> activeTokens = deviceTokenRepository.findAllActiveByUserId(userId);
        if (activeTokens.isEmpty()) {
            return;
        }

        for (DeviceToken deviceToken : activeTokens) {
            sendToSingleToken(deviceToken, title, body, recordId, type);
        }
    }

    private void sendToSingleToken(DeviceToken deviceToken, String title, String body, UUID recordId, NotificationType type) {
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("FirebaseApp kurulu değil, push bildirimi atlanıyor. Token: {}", deviceToken.getToken());
            return;
        }

        Message message = Message.builder()
                .setToken(deviceToken.getToken())
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putData("recordId", recordId.toString())
                .putData("type", type.name())
                .build();

        try {
            FirebaseMessaging.getInstance().send(message);
            log.info("Push bildirimi gönderildi -> User: {}, Token: {}", deviceToken.getUser().getId(), deviceToken.getToken());
        } catch (FirebaseMessagingException e) {
            handleFcmError(e, deviceToken.getToken());
        } catch (Exception e) {
            log.error("Push bildirimi gönderilirken beklenmeyen hata oluştu: {}", e.getMessage());
        }
    }

    private void handleFcmError(FirebaseMessagingException e, String token) {
        MessagingErrorCode errorCode = e.getMessagingErrorCode();
        log.warn("FCM gönderim hatası: {} (Kod: {})", e.getMessage(), errorCode);

        if (errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
            log.info("Geçersiz / silinmiş FCM token tespit edildi, pasifleştiriliyor: {}", token);
            deviceTokenRepository.deactivateByToken(token);
        }
    }
}