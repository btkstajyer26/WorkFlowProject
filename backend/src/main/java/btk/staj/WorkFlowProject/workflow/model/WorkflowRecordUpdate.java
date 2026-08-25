package btk.staj.WorkFlowProject.workflow.model;

import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Persistence-neutral command describing a validated record transition update. */
public record WorkflowRecordUpdate(
        UUID recordId,
        RecordStatus newStatus,
        UUID assignedTo,
        UUID lastDeputyId,
        int expectedVersion,
        Instant updatedAt) {

    public WorkflowRecordUpdate {
        Objects.requireNonNull(recordId, "recordId");
        Objects.requireNonNull(newStatus, "newStatus");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }
}
