package btk.staj.WorkFlowProject.workflow.model;

import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable user data required while validating a workflow target.
 *
 * <p>{@code workflowActor} ve {@code permissionCodes}, hedefin inis durumunda islem
 * yapabildigini dogrulamak icin gerekir (ADR-0008 K4). Alanlar bilerek {@link
 * btk.staj.WorkFlowProject.workflow.model.CurrentActor} ile ayni sekildedir: aktor ve
 * hedef ayni yetenek sorusuna ayni veriyle cevap verir.
 */
public record WorkflowUserSnapshot(UUID id, RoleId roleId, boolean active,
                                   boolean workflowActor, Set<String> permissionCodes) {

    public WorkflowUserSnapshot {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(roleId, "roleId");
        permissionCodes = Set.copyOf(permissionCodes);
    }
}
