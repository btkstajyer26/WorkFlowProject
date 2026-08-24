package btk.staj.WorkFlowProject.notification.controller;

import btk.staj.WorkFlowProject.auth.security.AuthenticatedUser;
import btk.staj.WorkFlowProject.notification.dto.DeviceTokenDeleteRequest;
import btk.staj.WorkFlowProject.notification.dto.DeviceTokenRequest;
import btk.staj.WorkFlowProject.notification.service.DeviceTokenService;
import btk.staj.WorkFlowProject.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
<<<<<<< HEAD
import org.springframework.web.bind.annotation.*;
=======
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
>>>>>>> 50822890863c445b695dec8a6916822115a11898

import javax.sql.DataSource;
import java.util.UUID;

@RestController
@RequestMapping("/api/device-tokens")
@ConditionalOnBean(DataSource.class)
@RequiredArgsConstructor
@Tag(name = "Device Tokens", description = "Mobil cihaz push bildirim token yönetim uçları")
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    @PostMapping
    @Operation(summary = "Cihaz token kaydı / güncelleme (Upsert)")
    public ResponseEntity<Void> registerToken(
            Authentication authentication,
            @Valid @RequestBody DeviceTokenRequest request) {

        UUID userId = extractUserId(authentication);
        deviceTokenService.registerOrUpdateToken(userId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    @Operation(summary = "Cihaz tokenını pasifleştir (Sahiplik doğrulamalı)")
    public ResponseEntity<Void> removeToken(
            Authentication authentication,
            @Valid @RequestBody DeviceTokenDeleteRequest request) {

        UUID userId = extractUserId(authentication);
        deviceTokenService.deactivateTokenForUser(userId, request.getToken());
        return ResponseEntity.noContent().build();
    }

<<<<<<< HEAD
=======
    /**
     * JWT filtresi principal olarak {@link AuthenticatedUser} koyar.
     * {@link User} / {@link UUID} fallback'leri test veya alternatif baglamlar icindir.
     */
>>>>>>> 50822890863c445b695dec8a6916822115a11898
    private UUID extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("Kimlik doğrulaması bulunamadı.");
        }

        Object principal = authentication.getPrincipal();

<<<<<<< HEAD
        if (principal instanceof User user) {
            return user.getId();
        } else if (principal instanceof UUID uuid) {
=======
        if (principal instanceof AuthenticatedUser authenticatedUser) {
            return authenticatedUser.getId();
        }
        if (principal instanceof User user) {
            return user.getId();
        }
        if (principal instanceof UUID uuid) {
>>>>>>> 50822890863c445b695dec8a6916822115a11898
            return uuid;
        }

        throw new IllegalArgumentException("Geçersiz kullanıcı kimliği.");
    }
<<<<<<< HEAD
}
=======
}
>>>>>>> 50822890863c445b695dec8a6916822115a11898
