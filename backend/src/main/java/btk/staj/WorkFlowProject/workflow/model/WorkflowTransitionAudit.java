package btk.staj.WorkFlowProject.workflow.model;

import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Semantic audit information emitted for one successful workflow transition.
 *
 * <p>Atama, gecisin NEREDEN NEREYE tasidigini gosterecek sekilde iki yanli
 * tasinir (B12 / ADR-0009). Her yan ya bir kisiye, ya bir departmana, ya da
 * hicbir yere isaret eder; ikisi ayni anda dolu olamaz. Atama TURU
 * ({@code USER}/{@code DEPARTMENT}/{@code NONE}) burada saklanmaz, okuma
 * aninda {@code AssignmentView.of(...)} ile turetilir - turetimin tek
 * dogruluk kaynagi orasidir ve {@code records} tablosu da ayni sekli kullanir.
 *
 * <p>Alan adlari {@code audit_logs} kolon adlariyla birebir hizalidir; bu,
 * {@code AuditLogService.record} map'lemesini gozle dogrulanabilir kilar.
 */
public record WorkflowTransitionAudit(
        UUID recordId,
        WorkflowAction action,
        RecordStatus previousStatus,
        RecordStatus newStatus,
        UUID actorId,
        RoleId actorRoleId,
        UUID previousAssignedTo,
        Integer previousAssignedDepartmentId,
        UUID newAssignedTo,
        Integer newAssignedDepartmentId,
        String comment,
        Instant performedAt) {

    public WorkflowTransitionAudit {
        // Kalip WorkflowRecordUpdate ile ayni: iliski hatasi requireNonNull'lardan
        // ONCE atilir ki "iki atama birden" hatasi "null recordId"nin ardina
        // gizlenmesin. Iki yan icin ayri mesaj, cunku hangi yanin bozuk oldugu
        // tek mesajdan anlasilmazdi.
        if (previousAssignedTo != null && previousAssignedDepartmentId != null) {
            throw new IllegalArgumentException(
                    "Previous user and department assignments are mutually exclusive");
        }
        if (newAssignedTo != null && newAssignedDepartmentId != null) {
            throw new IllegalArgumentException(
                    "New user and department assignments are mutually exclusive");
        }
        Objects.requireNonNull(recordId, "recordId");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(previousStatus, "previousStatus");
        Objects.requireNonNull(newStatus, "newStatus");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(actorRoleId, "actorRoleId");
        Objects.requireNonNull(performedAt, "performedAt");
    }
}
