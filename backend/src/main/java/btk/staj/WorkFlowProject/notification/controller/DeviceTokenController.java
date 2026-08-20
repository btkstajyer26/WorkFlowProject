package btk.staj.WorkFlowProject.notification.controller;

import btk.staj.WorkFlowProject.notification.dto.DeviceTokenDeleteRequest;
import btk.staj.WorkFlowProject.notification.dto.DeviceTokenRequest;
import btk.staj.WorkFlowProject.notification.service.DeviceTokenService;
import btk.staj.WorkFlowProject.auth.security.AuthenticatedUser;
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
    @Operation(summary = "Cihaz tokenını pasifleştir")
    public ResponseEntity<Void> removeToken(@Valid @RequestBody DeviceTokenDeleteRequest request) {
        deviceTokenService.deactivateToken(request.getToken());
        return ResponseEntity.noContent().build();
    }

    /**
     * Projede kimlik dogrulanmis principal her zaman {@link AuthenticatedUser}
     * tipindedir; baska bir tip gelirse kullanici kimligi cozulemedigi icin
     * token sahipsiz kaydedilmemeli, istek hata ile durmalidir.
     */
    private UUID extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("Kimlik doğrulaması bulunamadı.");
        }
        if (authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser) {
            return authenticatedUser.getId();
        }
        throw new IllegalStateException("Kimlik doğrulanmış kullanıcı çözümlenemedi.");
    }
}