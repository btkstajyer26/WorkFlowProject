package btk.staj.WorkFlowProject.workflow.model;

import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Authenticated workflow actor without a dependency on a security framework. */
public record CurrentActor(UUID id, RoleId roleId, boolean workflowActor, Set<String> permissionCodes) {

    public CurrentActor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(roleId, "roleId");
        permissionCodes = Set.copyOf(permissionCodes);
    }
}
