package btk.staj.WorkFlowProject.rbac.service;

import btk.staj.WorkFlowProject.auth.security.AuthenticatedUser;
import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.support.WorkflowRoleFixtures;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.workflow.adapter.DbTransitionRuleSource;
import btk.staj.WorkFlowProject.workflow.exception.TransitionRuleConfigurationException;
import btk.staj.WorkFlowProject.workflow.model.TransitionRuleRecord;
import btk.staj.WorkFlowProject.workflow.statemachine.*;
import java.util.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class PermissionAuthorizationTest {
    private final StaticTransitionRuleSource rules = new StaticTransitionRuleSource(WorkflowRoleFixtures.roleIds());
    private final WorkflowTransitionValidator validator = new WorkflowTransitionValidator(rules);

    private TransitionContext context(RecordStatus status, boolean actor, Set<String> codes) {
        return new TransitionContext(
                status,
                WorkflowAction.ONAYLA,
                WorkflowRoleFixtures.id(RoleName.BASKAN),
                false,
                true,
                null,
                false,
                null,
                false,
                actor,
                codes);
    }

    @Test void permissionIsRequiredAndWrongPermissionCannotSubstitute() {
        for (Set<String> codes : List.of(Set.<String>of(), Set.of("ROLE_BASKAN"), Set.of("RECORD_REJECT"))) {
            assertThat(validator.validate(context(RecordStatus.BASKAN_INCELEMESINDE, true, codes)))
                    .isEqualTo(TransitionDecision.rejected(WorkflowErrorCode.WORKFLOW_FORBIDDEN));
        }
        assertThat(validator.validate(context(RecordStatus.BASKAN_INCELEMESINDE, true, Set.of("RECORD_APPROVE"))))
                .isEqualTo(TransitionDecision.allowed(RecordStatus.ONAYLANDI));
    }

    @Test void eligibilityAndTerminalErrorsKeepTheirPriority() {
        assertThat(validator.validate(context(RecordStatus.ONAYLANDI, false, Set.of())))
                .isEqualTo(TransitionDecision.rejected(WorkflowErrorCode.WORKFLOW_ROLE_NOT_ALLOWED));
        assertThat(validator.validate(context(RecordStatus.ONAYLANDI, true, Set.of())))
                .isEqualTo(TransitionDecision.rejected(WorkflowErrorCode.WORKFLOW_RECORD_LOCKED));
        assertThat(validator.validate(context(RecordStatus.TASLAK, true, Set.of())))
                .isEqualTo(TransitionDecision.rejected(WorkflowErrorCode.WORKFLOW_INVALID_TRANSITION));
    }

    @Test void activeRuleCannotOmitPermissionMetadata() {
        for (String code : Arrays.asList(null, "", " ")) {
            assertThatThrownBy(() -> new DbTransitionRuleSource(() -> List.of(new TransitionRuleRecord(
                    "BASKAN_INCELEMESINDE",
                    "ONAYLA",
                    WorkflowRoleFixtures.value(RoleName.BASKAN),
                    "ASSIGNEE",
                    "ONAYLANDI",
                    "NONE",
                    null,
                    code))))
                    .isInstanceOf(TransitionRuleConfigurationException.class)
                    .hasMessageContaining("requiredPermissionCode");
        }
    }

    @Test void createEditDeleteAreIndependentCapabilities() {
        PermissionService service = new PermissionService(rules);
        assertThat(service.canCreateRecord(Set.of("RECORD_CREATE"))).isTrue();
        assertThat(service.canEditRecord(Set.of("RECORD_CREATE"), RecordStatus.TASLAK)).isFalse();
        assertThat(service.canDeleteRecord(Set.of("RECORD_EDIT"), RecordStatus.TASLAK)).isFalse();
        assertThat(service.canEditRecord(Set.of("RECORD_EDIT"), RecordStatus.DUZENLEME_BEKLIYOR)).isTrue();
        assertThat(service.canDeleteRecord(Set.of("RECORD_DELETE"), RecordStatus.DUZENLEME_BEKLIYOR)).isFalse();
        assertThat(service.canDeleteRecord(Set.of("RECORD_DELETE"), RecordStatus.TASLAK)).isTrue();
        assertThat(service.canApprove(WorkflowRoleFixtures.id(RoleName.BASKAN), RecordStatus.BASKAN_INCELEMESINDE, Set.of())).isFalse();
    }

    @Test void principalCopiesPermissionsAndInactiveRoleIsDisabled() {
        Role role = new Role(); role.setActive(true); role.setName("Yeni rol");
        User user = new User(); user.setRole(role);
        Set<String> codes = new HashSet<>(Set.of("USER_VIEW"));
        AuthenticatedUser principal = new AuthenticatedUser(user, codes);
        codes.add("USER_MANAGE");
        assertThat(principal.getPermissionCodes()).containsExactly("USER_VIEW");
        assertThatThrownBy(() -> principal.getPermissionCodes().add("ROLE_ADMIN"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(principal.getAuthorities()).extracting("authority").containsExactly("USER_VIEW");
        role.setActive(false);
        AuthenticatedUser disabled = new AuthenticatedUser(user, codes);
        assertThat(disabled.isEnabled()).isFalse();
        assertThat(disabled.getAuthorities()).isEmpty();
    }
}
