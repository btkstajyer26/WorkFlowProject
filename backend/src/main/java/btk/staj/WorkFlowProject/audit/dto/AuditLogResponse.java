package btk.staj.WorkFlowProject.audit.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Sartnamedeki "Islem Gecmisi" tablosunun (§4.2) bir satiri.
 *
 * <p>Tablo kullaniciyi ve rolu <em>adiyla</em> gosterir ("Ahmet Yilmaz",
 * "Baskan Yardimcisi"); bu yuzden ham {@code userId}/{@code roleId} yaninda
 * cozulmus adlar da tasinir. Adlar denetim izi okunurken tek sorguda
 * birlestirilir — satir basina ayri sorgu (N+1) yapilmaz.
 */
public record AuditLogResponse(
        UUID id,
        UUID recordId,
        UUID userId,
        String userFullName,
        Integer roleId,
        String roleName,
        String action,
        String previousStatus,
        String newStatus,
        String comment,
        LocalDateTime createdAt) {
}
