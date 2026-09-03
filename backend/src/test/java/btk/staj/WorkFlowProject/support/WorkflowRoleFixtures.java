package btk.staj.WorkFlowProject.support;

import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import btk.staj.WorkFlowProject.workflow.statemachine.StaticTransitionRuleSource;

import java.util.Map;
import java.util.stream.Collectors;

/** Synthetic identities for tests without PostgreSQL; never used to infer real DB IDs. */
public final class WorkflowRoleFixtures {
    private static final Map<RoleName, RoleId> IDS = Map.of(
            RoleName.CALISAN, new RoleId(1),
            RoleName.BASKAN_YARDIMCISI, new RoleId(2),
            RoleName.BASKAN, new RoleId(3),
            RoleName.ADMIN, new RoleId(4));

    private WorkflowRoleFixtures() { }

    public static Map<RoleName, RoleId> roleIds() { return IDS; }

    public static RoleId id(RoleName role) { return role == null ? null : IDS.get(role); }

    public static Integer value(RoleName role) { return role == null ? null : id(role).value(); }

    public static Map<RoleId, RoleName> legacyRoles() {
        return IDS.entrySet().stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getValue, Map.Entry::getKey));
    }

    public static StaticTransitionRuleSource rules() { return new StaticTransitionRuleSource(IDS); }
}
