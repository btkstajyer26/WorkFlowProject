package btk.staj.WorkFlowProject.auth.security;

import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;

import java.util.Objects;
import java.util.UUID;

/** Legacy visibility identity, kept separate from workflow role IDs until DB-8. */
public record VisibilityActor(UUID id, RoleName role) {
    public VisibilityActor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(role, "role");
    }
}
