package btk.staj.WorkFlowProject.user.dto;

import btk.staj.WorkFlowProject.user.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Kullanici bilgilerinin API yanitinda donen bicimi. User entity'si dogrudan
 * donulmemelidir; passwordHash gibi alanlar disari sizar.
 *
 * <p>Rol uc alanla tasinir ve ucu ayri sorulara cevap verir:
 *
 * <ul>
 *   <li>{@code roleId} — iliskisel kimlik.</li>
 *   <li>{@code systemKey} — yerlesik rolun <strong>degismez</strong> teknik anahtari
 *       (V12 / DB-1 SS6.1). Panelden acilan dinamik rollerde {@code null}'dur.
 *       Istemci davranis secimlerini bu alana gore yapmalidir.</li>
 *   <li>{@code roleName} — <strong>gosterim adi</strong>. AP-2 ile panelden
 *       degistirilebilir; kimlik degildir ve sabit bir listeye karsi
 *       dogrulanmamalidir.</li>
 * </ul>
 */
public record UserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        Integer roleId,
        String systemKey,
        String roleName,
        boolean active,
        LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().getId() : null,
                user.getRole() != null ? user.getRole().getSystemKey() : null,
                user.getRole() != null ? user.getRole().getName() : null,
                user.isActive(),
                user.getCreatedAt());
    }
}
