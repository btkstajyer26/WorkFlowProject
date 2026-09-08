package btk.staj.WorkFlowProject.user.dto;

import btk.staj.WorkFlowProject.auth.security.AuthenticatedUser;
import btk.staj.WorkFlowProject.user.entity.User;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Oturum acmis kullanicinin mobil/web istemciler tarafindan kullanilan profili.
 *
 * <p>Rol kimligi ve gosterim adi ayridir:
 * {@code roleId} iliskisel kimlik, {@code systemKey} yerlesik rollerin degismez
 * teknik anahtari, {@code roleName} ise degistirilebilir gosterim adidir.
 *
 * <p>Yetki gerektiren istemci davranislari rol adindan tahmin edilmez;
 * {@code permissionCodes} backend tarafindan hesaplanan aktif yetkileri tasir.
 */
public record CurrentUserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        Integer roleId,
        String systemKey,
        String roleName,
        Set<String> permissionCodes,
        boolean active,
        LocalDateTime createdAt) {

    public static CurrentUserResponse from(AuthenticatedUser currentUser) {
        User user = currentUser.getUser();

        return new CurrentUserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().getId() : null,
                user.getRole() != null ? user.getRole().getSystemKey() : null,
                user.getRole() != null ? user.getRole().getName() : null,
                currentUser.getPermissionCodes(),
                user.isActive(),
                user.getCreatedAt());
    }
}
