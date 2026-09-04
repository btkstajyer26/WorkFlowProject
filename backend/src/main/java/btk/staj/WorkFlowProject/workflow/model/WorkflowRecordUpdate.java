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
        Instant updatedAt,
        Integer assignedDepartmentId) {

    public WorkflowRecordUpdate(UUID recordId, RecordStatus newStatus, UUID assignedTo,
            UUID lastDeputyId, int expectedVersion, Instant updatedAt) {
        this(recordId, newStatus, assignedTo, lastDeputyId, expectedVersion, updatedAt, null);
    }

    public WorkflowRecordUpdate {
        if (assignedTo != null && assignedDepartmentId != null) {
            throw new IllegalArgumentException("User and department assignments are mutually exclusive");
        }
        Objects.requireNonNull(recordId, "recordId");
        Objects.requireNonNull(newStatus, "newStatus");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }
}
