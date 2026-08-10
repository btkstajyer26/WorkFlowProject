package btk.staj.WorkFlowProject.workflow.model;

import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Semantic audit information emitted for one successful workflow transition. */
public record WorkflowTransitionAudit(
        UUID recordId,
        WorkflowAction action,
        RecordStatus previousStatus,
        RecordStatus newStatus,
        UUID actorId,
        RoleName actorRole,
        UUID assignedTo,
        String comment,
        Instant performedAt) {

    public WorkflowTransitionAudit {
        Objects.requireNonNull(recordId, "recordId");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(previousStatus, "previousStatus");
        Objects.requireNonNull(newStatus, "newStatus");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(actorRole, "actorRole");
        Objects.requireNonNull(performedAt, "performedAt");
    }
}
