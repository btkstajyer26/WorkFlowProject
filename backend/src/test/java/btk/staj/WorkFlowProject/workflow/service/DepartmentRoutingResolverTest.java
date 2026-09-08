package btk.staj.WorkFlowProject.workflow.service;

import btk.staj.WorkFlowProject.support.AuthorizationFixtures;
import btk.staj.WorkFlowProject.support.WorkflowRoleFixtures;
import btk.staj.WorkFlowProject.workflow.model.DepartmentRoutingResolution;
import btk.staj.WorkFlowProject.workflow.port.DepartmentRoutingPort;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import btk.staj.WorkFlowProject.workflow.statemachine.TransitionRuleSource;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DepartmentRoutingResolverTest {

    private static final int DEPARTMENT = 7;
    private static final UUID CREATOR =
            UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID ELIGIBLE =
            UUID.fromString("00000000-0000-0000-0000-0000000000d1");

    private final TransitionRuleSource rules = WorkflowRoleFixtures.rules();

    @Test
    void resolvesEligibleDepartmentAssigneesFromWorkflowRouting() {
        DepartmentRoutingResolver resolver =
                new DepartmentRoutingResolver(new StubRouting(true));

        assertThat(resolver.eligibleAssignees(
                DEPARTMENT,
                RecordStatus.BSK_YRD_INCELEMESINDE,
                rules))
                .containsExactly(ELIGIBLE);
    }


    @Test
    void excludesDepartmentMembersWhenTargetRoleLacksRequiredPermissions() {
        DepartmentRoutingResolver resolver =
                new DepartmentRoutingResolver(new StubRouting(false));

        assertThat(resolver.eligibleAssignees(
                DEPARTMENT,
                RecordStatus.BSK_YRD_INCELEMESINDE,
                rules))
                .isEmpty();
    }

    private static final class StubRouting implements DepartmentRoutingPort {

        private final boolean permissionsGranted;

        private StubRouting(boolean permissionsGranted) {
            this.permissionsGranted = permissionsGranted;
        }

        @Override
        public DepartmentRoutingResolution resolve(
                int departmentId, RecordStatus from, WorkflowAction action) {
            return new DepartmentRoutingResolution.Resolved(
                    WorkflowRoleFixtures.id(RoleName.BASKAN_YARDIMCISI),
                    Set.of(ELIGIBLE));
        }

        @Override
        public boolean isActiveDepartment(int departmentId) {
            return departmentId == DEPARTMENT;
        }

        @Override
        public Set<Integer> activeDepartmentIdsFor(UUID userId) {
            return Set.of(DEPARTMENT);
        }

        @Override
        public Set<Integer> activeDepartmentIds() {
            return Set.of(DEPARTMENT);
        }

        @Override
        public boolean roleHasPermission(RoleId roleId, String permissionCode) {
            return permissionsGranted
                    && AuthorizationFixtures.permissions(RoleName.BASKAN_YARDIMCISI)
                            .contains(permissionCode);
        }
    }
}
