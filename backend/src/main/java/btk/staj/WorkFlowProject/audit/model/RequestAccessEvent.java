package btk.staj.WorkFlowProject.audit.model;

import btk.staj.WorkFlowProject.rbac.SystemRoleKey;
import java.util.UUID;

/**
 * Tek bir HTTP isteğinin denetim izine yazılacak özeti.
 * Tablo seçimi (audit_logs / user_audit_logs) sistem rol anahtarına göre yapılır.
 */
public record RequestAccessEvent(
        String action,
        UUID userId,
        Integer roleId,
        String systemKey,
        String httpMethod,
        String requestPath,
        Integer httpStatus,
        String errorCode,
        String comment) {

    public boolean adminActor() {
        return SystemRoleKey.ADMIN.name().equals(systemKey);
    }
}
