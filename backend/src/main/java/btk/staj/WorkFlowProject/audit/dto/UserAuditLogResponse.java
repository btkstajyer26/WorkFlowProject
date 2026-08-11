package btk.staj.WorkFlowProject.audit.dto;

import btk.staj.WorkFlowProject.audit.entity.UserAuditLog;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Kullanici/rol degisikligi denetim izinin API'de donen bicimi. Entity dogrudan
 * donulmez (katmanli mimari kriteri).
 */
public record UserAuditLogResponse(
        UUID id,
        UUID targetUserId,
        UUID performedBy,
        String action,
        Integer previousRoleId,
        Integer newRoleId,
        Boolean previousActive,
        Boolean newActive,
        String comment,
        LocalDateTime createdAt) {

    public static UserAuditLogResponse from(UserAuditLog log) {
        return new UserAuditLogResponse(
                log.getId(),
                log.getTargetUserId(),
                log.getPerformedBy(),
                log.getAction(),
                log.getPreviousRoleId(),
                log.getNewRoleId(),
                log.getPreviousActive(),
                log.getNewActive(),
                log.getComment(),
                log.getCreatedAt());
    }
}
