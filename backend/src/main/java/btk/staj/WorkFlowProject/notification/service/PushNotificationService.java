package btk.staj.WorkFlowProject.notification.service;

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
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnBean(DataSource.class)
@RequiredArgsConstructor
public class PushNotificationService {

    private final DeviceTokenRepository deviceTokenRepository;

    @Value("${fcm.project-id:}")
    private String fcmProjectId;

    @Value("${fcm.client-email:}")
    private String fcmClientEmail;

    @Value("${fcm.private-key:}")
    private String fcmPrivateKey;

    private boolean fcmEnabled = false;

    @PostConstruct
    public void init() {
        if (fcmProjectId == null || fcmProjectId.isBlank() ||
            fcmClientEmail == null || fcmClientEmail.isBlank() ||
            fcmPrivateKey == null || fcmPrivateKey.isBlank()) {
            log.warn("FCM credentials eksik veya tanımlanmamış. Push bildirim servisi devre dışı.");
            fcmEnabled = false;
            return;
        }

        try {
            if (FirebaseApp.getApps().isEmpty()) {
                String fixedKey = fcmPrivateKey.replace("\\n", "\n");
                String jsonCredentials = String.format("""
                    {
                      "type": "service_account",
                      "project_id": "%s",
                      "client_email": "%s",
                      "private_key": "%s"
                    }
                    """, fcmProjectId, fcmClientEmail, fixedKey);

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(
                                new ByteArrayInputStream(jsonCredentials.getBytes(StandardCharsets.UTF_8))))
                        .build();

                FirebaseApp.initializeApp(options);
            }
            fcmEnabled = true;
            log.info("FCM başarıyla başlatıldı.");
        } catch (Exception e) {
            log.error("FCM başlatılırken hata oluştu: {}", e.getMessage());
            fcmEnabled = false;
        }
    }

    @Async
    public void sendPushNotification(UUID recipientId, String title, String pushBody, UUID recordId, NotificationType type) {
        if (!fcmEnabled) {
            return;
        }

        List<String> tokens = deviceTokenRepository.findActiveTokensByUserId(recipientId);
        if (tokens == null || tokens.isEmpty()) {
            return;
        }

        for (String token : tokens) {
            try {
                Message message = Message.builder()
                        .setToken(token)
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(pushBody)
                                .build())
                        .putData("recordId", recordId != null ? recordId.toString() : "")
                        .putData("type", type != null ? type.name() : "")
                        .build();

                FirebaseMessaging.getInstance().send(message);
            } catch (FirebaseMessagingException e) {
                log.warn("FCM gönderim hatası (token: {}): {}", token, e.getMessage());
                handleFcmError(e, token);
            } catch (Exception e) {
                log.error("Beklenmeyen push bildirim hatası: {}", e.getMessage());
            }
        }
    }

    private void handleFcmError(FirebaseMessagingException e, String token) {
        MessagingErrorCode errorCode = e.getMessagingErrorCode();
        if (errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
            log.info("Geçersiz FCM token pasifleştiriliyor: {}", token);
            deviceTokenRepository.deactivateByToken(token);
        }
    }
}