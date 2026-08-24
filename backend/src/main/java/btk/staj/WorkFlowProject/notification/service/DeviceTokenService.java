package btk.staj.WorkFlowProject.notification.service;

import btk.staj.WorkFlowProject.notification.dto.DeviceTokenRequest;
import btk.staj.WorkFlowProject.notification.entity.DeviceToken;
import btk.staj.WorkFlowProject.notification.repository.DeviceTokenRepository;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public void registerOrUpdateToken(UUID userId, DeviceTokenRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("Kullanıcı kimliği doğrulanamadı.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı: " + userId));

        DeviceToken deviceToken = deviceTokenRepository.findByToken(request.getToken())
                .orElseGet(() -> DeviceToken.builder()
                        .token(request.getToken())
                        .build());

        deviceToken.setUser(user);
        deviceToken.setPlatform(request.getPlatform());
        deviceToken.setDeviceName(request.getDeviceName());
        deviceToken.setActive(true);

        deviceTokenRepository.save(deviceToken);
        log.info("Cihaz token'ı kaydedildi/güncellendi. Kullanıcı: {}, Platform: {}", userId, request.getPlatform());
    }

    @Transactional
    public void deactivateTokenForUser(UUID userId, String token) {
        if (userId == null || token == null || token.isBlank()) {
            return;
        }
        int updated = deviceTokenRepository.deactivateByTokenAndUserId(token, userId);
        if (updated > 0) {
            log.info("Cihaz token'ı pasifleştirildi. Kullanıcı: {}, Token: {}", userId, maskToken(token));
        } else {
            log.warn("Cihaz token'ı pasifleştirilemedi (kullanıcı eşleşmedi). Kullanıcı: {}", userId);
        }
    }

    @Transactional
    public void deactivateToken(String token) {
        if (token != null && !token.isBlank()) {
            deviceTokenRepository.deactivateByToken(token);
            log.info("Cihaz token'ı pasifleştirildi: {}", maskToken(token));
        }
    }

    public static String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }
}