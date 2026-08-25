package btk.staj.WorkFlowProject.workflow.model;

import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;

import java.util.Objects;
import java.util.UUID;

/** Immutable user data required while validating a workflow target. */
public record WorkflowUserSnapshot(UUID id, RoleName role, boolean active) {

    public WorkflowUserSnapshot {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(role, "role");
    }
}
