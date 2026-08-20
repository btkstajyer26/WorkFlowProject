package btk.staj.WorkFlowProject.notification.service;

import btk.staj.WorkFlowProject.notification.dto.DeviceTokenRequest;
import btk.staj.WorkFlowProject.notification.entity.DeviceToken;
import btk.staj.WorkFlowProject.notification.repository.DeviceTokenRepository;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public void registerOrUpdateToken(UUID userId, DeviceTokenRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı: " + userId));

        DeviceToken deviceToken = deviceTokenRepository.findByToken(request.getToken())
                .map(existing -> {
                    // Upsert Kuralı: user_id, platform, deviceName, is_active ve updated_at güncellenir
                    existing.setUser(user);
                    existing.setPlatform(request.getPlatform().toUpperCase());
                    existing.setDeviceName(request.getDeviceName());
                    existing.setActive(true);
                    existing.setUpdatedAt(LocalDateTime.now());
                    return existing;
                })
                .orElseGet(() -> DeviceToken.builder()
                        .user(user)
                        .token(request.getToken())
                        .platform(request.getPlatform().toUpperCase())
                        .deviceName(request.getDeviceName())
                        .active(true)
                        .build());

        deviceTokenRepository.save(deviceToken);
        log.info("Cihaz token'ı kaydedildi/güncellendi. Kullanıcı: {}, Platform: {}", user.getEmail(), request.getPlatform());
    }

    @Transactional
    public void deactivateToken(String token) {
        deviceTokenRepository.deactivateByToken(token);
        log.info("Cihaz token'ı pasifleştirildi. Token: {}", token);
    }
}