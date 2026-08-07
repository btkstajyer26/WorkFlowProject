package btk.staj.WorkFlowProject.workflow.service;

import btk.staj.WorkFlowProject.workflow.model.TargetResolution;
import btk.staj.WorkFlowProject.workflow.model.TargetResolution.DataIntegrityReason;
import btk.staj.WorkFlowProject.workflow.model.WorkflowRecordSnapshot;
import btk.staj.WorkFlowProject.workflow.model.WorkflowUserSnapshot;
import btk.staj.WorkFlowProject.workflow.port.WorkflowUserPort;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class TargetUserResolverTest {

    private static final UUID RECORD_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CREATOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ASSIGNEE_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID LAST_DEPUTY_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID REQUESTED_TARGET_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID PRESIDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000006");
    private static final UUID SECOND_PRESIDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000007");

    private final WorkflowUserPort userPort = mock(WorkflowUserPort.class);
    private final TargetUserResolver resolver = new TargetUserResolver(userPort);

    @ParameterizedTest
    @EnumSource(value = WorkflowAction.class, names = {"GONDER", "TEKRAR_GONDER"})
    void requestTargetActionsReturnNotProvidedWhenRequestIdIsMissing(WorkflowAction action) {
        TargetResolution result = resolver.resolve(action, null, record(LAST_DEPUTY_ID));

        assertThat(result).isEqualTo(new TargetResolution.NotProvided());
        verifyNoInteractions(userPort);
    }

    @ParameterizedTest
    @EnumSource(value = WorkflowAction.class, names = {"GONDER", "TEKRAR_GONDER"})
    void requestTargetActionsReturnRequestTargetNotFoundWhenLookupIsEmpty(WorkflowAction action) {
        when(userPort.findById(REQUESTED_TARGET_ID)).thenReturn(Optional.empty());

        TargetResolution result = resolver.resolve(action, REQUESTED_TARGET_ID, record(LAST_DEPUTY_ID));

        assertThat(result).isEqualTo(new TargetResolution.RequestTargetNotFound(REQUESTED_TARGET_ID));
        verify(userPort).findById(REQUESTED_TARGET_ID);
        verifyNoMoreInteractions(userPort);
    }

    @ParameterizedTest
    @EnumSource(value = WorkflowAction.class, names = {"GONDER", "TEKRAR_GONDER"})
    void requestTargetActionsReturnResolvedUser(WorkflowAction action) {
        WorkflowUserSnapshot target = user(REQUESTED_TARGET_ID, RoleName.BASKAN_YARDIMCISI, true);
        when(userPort.findById(REQUESTED_TARGET_ID)).thenReturn(Optional.of(target));

        TargetResolution result = resolver.resolve(action, REQUESTED_TARGET_ID, record(LAST_DEPUTY_ID));

        assertThat(result).isEqualTo(new TargetResolution.Resolved(target));
    }

    @Test
    void requestTargetWithWrongRoleRemainsResolvedForValidator() {
        WorkflowUserSnapshot wrongRole = user(REQUESTED_TARGET_ID, RoleName.BASKAN, true);
        when(userPort.findById(REQUESTED_TARGET_ID)).thenReturn(Optional.of(wrongRole));

        TargetResolution result = resolver.resolve(
                WorkflowAction.GONDER,
                REQUESTED_TARGET_ID,
                record(LAST_DEPUTY_ID));

        assertThat(result).isEqualTo(new TargetResolution.Resolved(wrongRole));
    }

    @Test
    void inactiveRequestTargetRemainsResolvedForValidator() {
        WorkflowUserSnapshot inactive = user(REQUESTED_TARGET_ID, RoleName.BASKAN_YARDIMCISI, false);
        when(userPort.findById(REQUESTED_TARGET_ID)).thenReturn(Optional.of(inactive));

        TargetResolution result = resolver.resolve(
                WorkflowAction.TEKRAR_GONDER,
                REQUESTED_TARGET_ID,
                record(LAST_DEPUTY_ID));

        assertThat(result).isEqualTo(new TargetResolution.Resolved(inactive));
    }

    @Test
    void baskanaIletResolvesTheSingleActivePresident() {
        WorkflowUserSnapshot president = user(PRESIDENT_ID, RoleName.BASKAN, true);
        when(userPort.findActiveByRole(RoleName.BASKAN)).thenReturn(List.of(president));

        TargetResolution result = resolver.resolve(
                WorkflowAction.BASKANA_ILET,
                REQUESTED_TARGET_ID,
                record(LAST_DEPUTY_ID));

        assertThat(result).isEqualTo(new TargetResolution.Resolved(president));
        verify(userPort).findActiveByRole(RoleName.BASKAN);
        verifyNoMoreInteractions(userPort);
    }

    @Test
    void baskanaIletReturnsRoleNotConfiguredWhenNoActivePresidentExists() {
        when(userPort.findActiveByRole(RoleName.BASKAN)).thenReturn(List.of());

        TargetResolution result = resolver.resolve(
                WorkflowAction.BASKANA_ILET,
                null,
                record(LAST_DEPUTY_ID));

        assertThat(result).isEqualTo(new TargetResolution.RoleNotConfigured(RoleName.BASKAN, 0));
    }

    @Test
    void baskanaIletReturnsRoleNotConfiguredWhenMultipleActivePresidentsExist() {
        WorkflowUserSnapshot first = user(PRESIDENT_ID, RoleName.BASKAN, true);
        WorkflowUserSnapshot second = user(SECOND_PRESIDENT_ID, RoleName.BASKAN, true);
        when(userPort.findActiveByRole(RoleName.BASKAN)).thenReturn(List.of(first, second));

        TargetResolution result = resolver.resolve(
                WorkflowAction.BASKANA_ILET,
                null,
                record(LAST_DEPUTY_ID));

        assertThat(result).isEqualTo(new TargetResolution.RoleNotConfigured(RoleName.BASKAN, 2));
    }

    @Test
    void singlePresidentPortResultRemainsResolvedForValidatorWithoutRevalidation() {
        WorkflowUserSnapshot inconsistentSnapshot = user(PRESIDENT_ID, RoleName.ADMIN, false);
        when(userPort.findActiveByRole(RoleName.BASKAN)).thenReturn(List.of(inconsistentSnapshot));

        TargetResolution result = resolver.resolve(
                WorkflowAction.BASKANA_ILET,
                null,
                record(LAST_DEPUTY_ID));

        assertThat(result).isEqualTo(new TargetResolution.Resolved(inconsistentSnapshot));
    }

    @Test
    void calisanaGeriGonderResolvesCreatedByAndIgnoresRequestTarget() {
        WorkflowUserSnapshot creator = user(CREATOR_ID, RoleName.CALISAN, true);
        when(userPort.findById(CREATOR_ID)).thenReturn(Optional.of(creator));

        TargetResolution result = resolver.resolve(
                WorkflowAction.CALISANA_GERI_GONDER,
                REQUESTED_TARGET_ID,
                record(LAST_DEPUTY_ID));

        assertThat(result).isEqualTo(new TargetResolution.Resolved(creator));
        verify(userPort).findById(CREATOR_ID);
        verifyNoMoreInteractions(userPort);
    }

    @Test
    void calisanaGeriGonderReturnsDataIntegrityFailureWhenCreatorUserIsMissing() {
        when(userPort.findById(CREATOR_ID)).thenReturn(Optional.empty());

        TargetResolution result = resolver.resolve(
                WorkflowAction.CALISANA_GERI_GONDER,
                null,
                record(LAST_DEPUTY_ID));

        assertThat(result).isEqualTo(new TargetResolution.DataIntegrityFailure(
                DataIntegrityReason.CREATED_BY_USER_NOT_FOUND,
                CREATOR_ID));
    }

    @Test
    void baskanYardimcisinaGeriGonderReturnsDataIntegrityFailureWhenLastDeputyIdIsMissing() {
        TargetResolution result = resolver.resolve(
                WorkflowAction.BASKAN_YARDIMCISINA_GERI_GONDER,
                null,
                record(null));

        assertThat(result).isEqualTo(new TargetResolution.DataIntegrityFailure(
                DataIntegrityReason.LAST_DEPUTY_ID_MISSING,
                null));
        verifyNoInteractions(userPort);
    }

    @Test
    void baskanYardimcisinaGeriGonderReturnsDataIntegrityFailureWhenDeputyUserIsMissing() {
        when(userPort.findById(LAST_DEPUTY_ID)).thenReturn(Optional.empty());

        TargetResolution result = resolver.resolve(
                WorkflowAction.BASKAN_YARDIMCISINA_GERI_GONDER,
                null,
                record(LAST_DEPUTY_ID));

        assertThat(result).isEqualTo(new TargetResolution.DataIntegrityFailure(
                DataIntegrityReason.LAST_DEPUTY_USER_NOT_FOUND,
                LAST_DEPUTY_ID));
    }

    @Test
    void baskanYardimcisinaGeriGonderResolvesLastDeputy() {
        WorkflowUserSnapshot deputy = user(LAST_DEPUTY_ID, RoleName.BASKAN_YARDIMCISI, true);
        when(userPort.findById(LAST_DEPUTY_ID)).thenReturn(Optional.of(deputy));

        TargetResolution result = resolver.resolve(
                WorkflowAction.BASKAN_YARDIMCISINA_GERI_GONDER,
                REQUESTED_TARGET_ID,
                record(LAST_DEPUTY_ID));

        assertThat(result).isEqualTo(new TargetResolution.Resolved(deputy));
        verify(userPort).findById(LAST_DEPUTY_ID);
        verifyNoMoreInteractions(userPort);
    }

    @ParameterizedTest
    @EnumSource(value = WorkflowAction.class, names = {"ONAYLA", "REDDET"})
    void terminalActionsReturnNotProvidedWithoutLookingUpRequestTarget(WorkflowAction action) {
        TargetResolution result = resolver.resolve(action, REQUESTED_TARGET_ID, record(LAST_DEPUTY_ID));

        assertThat(result).isEqualTo(new TargetResolution.NotProvided());
        verifyNoInteractions(userPort);
    }

    @Test
    void requiredInputsRejectNull() {
        assertThatNullPointerException()
                .isThrownBy(() -> new TargetUserResolver(null))
                .withMessage("userPort");
        assertThatNullPointerException()
                .isThrownBy(() -> resolver.resolve(null, null, record(LAST_DEPUTY_ID)))
                .withMessage("action");
        assertThatNullPointerException()
                .isThrownBy(() -> resolver.resolve(WorkflowAction.ONAYLA, null, null))
                .withMessage("record");
    }

    private static WorkflowUserSnapshot user(UUID id, RoleName role, boolean active) {
        return new WorkflowUserSnapshot(id, role, active);
    }

    private static WorkflowRecordSnapshot record(UUID lastDeputyId) {
        return new WorkflowRecordSnapshot(
                RECORD_ID,
                RecordStatus.TASLAK,
                CREATOR_ID,
                ASSIGNEE_ID,
                lastDeputyId,
                null,
                3);
    }
}
