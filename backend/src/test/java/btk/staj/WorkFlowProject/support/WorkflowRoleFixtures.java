package btk.staj.WorkFlowProject.support;


import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import btk.staj.WorkFlowProject.workflow.statemachine.StaticTransitionRuleSource;
import java.util.Map;
import java.util.UUID;
import btk.staj.WorkFlowProject.workflow.model.WorkflowUserSnapshot;

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

    public static StaticTransitionRuleSource rules() { return new StaticTransitionRuleSource(IDS); }

    /**
     * Rolunun gercek yetkileriyle bir hedef anlik goruntusu (ADR-0008 K4).
     *
     * <p>Hedefin inis durumunda islem yapabildigi kontrol edildigi icin testlerin
     * varsayilan hedefi <em>yetenekli</em> olmalidir; aksi halde her senaryo
     * WORKFLOW_TARGET_CANNOT_ACT'a takilirdi.
     */
    public static WorkflowUserSnapshot target(UUID id, RoleName role, boolean active) {
        return new WorkflowUserSnapshot(id, id(role), active,
                AuthorizationFixtures.workflowActor(role), AuthorizationFixtures.permissions(role));
    }
}
