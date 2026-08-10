package btk.staj.WorkFlowProject.workflow.model;

import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;

import java.util.Objects;
import java.util.UUID;

/** Authenticated workflow actor without a dependency on a security framework. */
public record CurrentActor(UUID id, RoleName role) {

    public CurrentActor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(role, "role");
    }
}
