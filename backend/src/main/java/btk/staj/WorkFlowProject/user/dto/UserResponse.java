package btk.staj.WorkFlowProject.user.dto;

import btk.staj.WorkFlowProject.user.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Kullanici bilgilerinin API yanitinda donen bicimi. User entity'si dogrudan
 * donulmemelidir; passwordHash gibi alanlar disari sizar.
 */
public record UserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String roleName,
        boolean active,
        LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().getName() : null,
                user.isActive(),
                user.getCreatedAt());
    }
}