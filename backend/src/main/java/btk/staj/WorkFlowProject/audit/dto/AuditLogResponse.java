package btk.staj.WorkFlowProject.audit.dto;

import java.time.LocalDateTime;
import java.util.UUID;


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
        String httpMethod,
        String requestPath,
        Integer httpStatus,
        String errorCode,
        LocalDateTime createdAt) {
}
