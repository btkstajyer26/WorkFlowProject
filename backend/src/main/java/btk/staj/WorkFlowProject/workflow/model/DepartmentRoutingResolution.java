package btk.staj.WorkFlowProject.workflow.model;

import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import java.util.Set;
import java.util.UUID;

/** Department routing is an actor pool, never an arbitrary selected user. */
public sealed interface DepartmentRoutingResolution {
    record NotDepartmentAssigned() implements DepartmentRoutingResolution {}
    record Resolved(RoleId targetRoleId, Set<UUID> eligibleUserIds) implements DepartmentRoutingResolution {
        public Resolved { eligibleUserIds = Set.copyOf(eligibleUserIds); }
    }
    record RuleNotConfigured(int departmentId) implements DepartmentRoutingResolution {}
    record NoEligibleMember(int departmentId, RoleId targetRoleId) implements DepartmentRoutingResolution {}
}
