package btk.staj.WorkFlowProject.audit.dto;

import btk.staj.WorkFlowProject.common.dto.AssignmentView;

import java.time.LocalDateTime;
import java.util.UUID;


/**
 * Bir denetim izi satirinin istemciye acilan hali.
 *
 * <p>Atama (B12 / ADR-0009) ham kimliklerle degil, ortak {@link AssignmentView}
 * sozlesmesiyle tasinir: istemci atama turunu iki nullable alani
 * karsilastirarak <em>cikarsamaz</em> (APP-9/APP-10/B11 SS3.1). Iki alan
 * gecisin atamayi nereden nereye tasidigini gosterir; gecis olmayan satirlarda
 * (yasam dongusu olaylari, HTTP erisim loglari) ikisi de {@code NONE}'dir.
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
        AssignmentView previousAssignment,
        AssignmentView newAssignment,
        String httpMethod,
        String requestPath,
        Integer httpStatus,
        String errorCode,
        LocalDateTime createdAt) {

    /**
     * Repository sorgusunun cagirdigi kurucu: satirdan ham kimlikler gelir,
     * atama TURU burada {@link AssignmentView#of} ile turetilir. Turetimin tek
     * dogruluk kaynagi orasidir; sema bu turu saklamaz (ADR-0009 K1).
     *
     * <p>Gosterim adlari bu asamada bos kalir, {@link #withAssignments} ile
     * toplu olarak doldurulur.
     */
    public AuditLogResponse(UUID id,
                            UUID recordId,
                            UUID userId,
                            String userFullName,
                            Integer roleId,
                            String roleName,
                            String action,
                            String previousStatus,
                            String newStatus,
                            String comment,
                            UUID previousAssignedTo,
                            Integer previousAssignedDepartmentId,
                            UUID newAssignedTo,
                            Integer newAssignedDepartmentId,
                            String httpMethod,
                            String requestPath,
                            Integer httpStatus,
                            String errorCode,
                            LocalDateTime createdAt) {
        this(id, recordId, userId, userFullName, roleId, roleName, action,
                previousStatus, newStatus, comment,
                AssignmentView.of(previousAssignedTo, previousAssignedDepartmentId),
                AssignmentView.of(newAssignedTo, newAssignedDepartmentId),
                httpMethod, requestPath, httpStatus, errorCode, createdAt);
    }

    /** Kimlikleri koruyarak gosterim adlarini yerlestirir. */
    public AuditLogResponse withAssignments(AssignmentView previous, AssignmentView current) {
        return new AuditLogResponse(id, recordId, userId, userFullName, roleId, roleName, action,
                previousStatus, newStatus, comment, previous, current,
                httpMethod, requestPath, httpStatus, errorCode, createdAt);
    }
}
