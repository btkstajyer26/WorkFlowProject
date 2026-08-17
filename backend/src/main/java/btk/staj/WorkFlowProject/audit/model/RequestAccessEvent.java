package btk.staj.WorkFlowProject.audit.model;

import java.util.UUID;

/**
 * Tek bir HTTP isteğinin denetim izine yazılacak özeti.
 * Tablo seçimi (audit_logs / user_audit_logs) rol adına göre yapılır.
 */
public record RequestAccessEvent(
        String action,
        UUID userId,
        Integer roleId,
        String roleName,
        String httpMethod,
        String requestPath,
        Integer httpStatus,
        String errorCode,
        String comment) {

    public boolean adminActor() {
        return "ADMIN".equals(roleName);
    }
}
