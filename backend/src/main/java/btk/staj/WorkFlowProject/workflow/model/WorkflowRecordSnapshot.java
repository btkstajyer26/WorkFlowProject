package btk.staj.WorkFlowProject.workflow.model;

import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Immutable projection of record state needed by the workflow application layer. */
public record WorkflowRecordSnapshot(
        UUID id,
        RecordStatus status,
        UUID createdBy,
        UUID assignedTo,
        UUID lastDeputyId,
        Instant deletedAt,
        int version) {

    public WorkflowRecordSnapshot {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdBy, "createdBy");
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
