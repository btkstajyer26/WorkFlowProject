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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/device-tokens")
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

    /**
     * JWT filtresi principal olarak {@link AuthenticatedUser} koyar; gercek
     * isteklerde her zaman bu dal calisir. {@link AuthenticatedUser} bir
     * {@link User} <em>degildir</em>, onu sarar (implements UserDetails) —
     * bu yuzden ayri bir dal gerekir, {@code instanceof User} eslesmez.
     * {@link User} / {@link UUID} fallback'leri test veya alternatif
     * baglamlar icindir.
     */
    private UUID extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("Kimlik doğrulaması bulunamadı.");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof AuthenticatedUser authenticatedUser) {
            return authenticatedUser.getId();
        } else if (principal instanceof User user) {
            return user.getId();
        } else if (principal instanceof UUID uuid) {
            return uuid;
        }

        throw new IllegalArgumentException("Geçersiz kullanıcı kimliği.");
    }
}
