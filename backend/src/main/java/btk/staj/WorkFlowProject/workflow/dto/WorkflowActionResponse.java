package btk.staj.WorkFlowProject.workflow.dto;

import btk.staj.WorkFlowProject.common.dto.AssignmentView;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;

import java.time.Instant;
import java.util.UUID;

/**
 * Backend-calculated result of a successful workflow transition.
 *
 * <p>{@code assignedTo} korunur ve {@link AssignmentView#userId()} ile ayni degeri tasir;
 * mevcut istemciler kirilmasin diye kaldirilmadi, ancak <strong>turetilmistir</strong> ve
 * emekliye ayrilacaktir. Yeni istemci kodu {@code assignment} uzerinden dallanir (B11 SS3.5).
 */
public record WorkflowActionResponse(
        UUID recordId,
        WorkflowAction action,
        RecordStatus previousStatus,
        RecordStatus newStatus,
        UUID assignedTo,
        AssignmentView assignment,
        int version,
        UUID performedBy,
        Instant performedAt) {

    /** Gosterim adlari cozulmus bir kopya; kimlikler ve gecis bilgisi degismez. */
    public WorkflowActionResponse withAssignmentNames(String userFullName, String departmentName) {
        return new WorkflowActionResponse(recordId, action, previousStatus, newStatus, assignedTo,
                assignment.withNames(userFullName, departmentName), version, performedBy, performedAt);
    }
}
