package btk.staj.WorkFlowProject.workflow.model;

import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Event payload published after a successful workflow status change. */
public record WorkflowStatusChangedEvent(
        UUID recordId,
        WorkflowAction action,
        RecordStatus previousStatus,
        RecordStatus newStatus,
        UUID actorId,
        RoleId actorRoleId,
        UUID previousAssignedTo,
        UUID assignedTo,
        String comment,
        Instant performedAt) {

    public WorkflowStatusChangedEvent {
        Objects.requireNonNull(recordId, "recordId");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(previousStatus, "previousStatus");
        Objects.requireNonNull(newStatus, "newStatus");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(actorRoleId, "actorRoleId");
        Objects.requireNonNull(performedAt, "performedAt");
    }
}
