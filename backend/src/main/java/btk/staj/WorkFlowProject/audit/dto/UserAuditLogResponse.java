package btk.staj.WorkFlowProject.audit.dto;

import btk.staj.WorkFlowProject.audit.entity.UserAuditLog;

import java.time.LocalDateTime;
import java.util.UUID;


public record UserAuditLogResponse(
        UUID id,
        UUID targetUserId,
        String targetUserFullName,
        UUID performedBy,
        String performedByFullName,
        String action,
        Integer previousRoleId,
        String previousRoleName,
        Integer newRoleId,
        String newRoleName,
        Boolean previousActive,
        Boolean newActive,
        String comment,
        LocalDateTime createdAt) {

    public static UserAuditLogResponse from(UserAuditLog log) {
        return new UserAuditLogResponse(
                log.getId(),
                log.getTargetUserId(),
                null,
                log.getPerformedBy(),
                null,
                log.getAction(),
                log.getPreviousRoleId(),
                null,
                log.getNewRoleId(),
                null,
                log.getPreviousActive(),
                log.getNewActive(),
                log.getComment(),
                log.getCreatedAt());
    }
}