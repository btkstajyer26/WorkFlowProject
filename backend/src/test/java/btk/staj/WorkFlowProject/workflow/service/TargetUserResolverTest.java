package btk.staj.WorkFlowProject.workflow.service;

import btk.staj.WorkFlowProject.support.WorkflowRoleFixtures;
import btk.staj.WorkFlowProject.workflow.model.TargetResolution.DataIntegrityReason;
import btk.staj.WorkFlowProject.workflow.model.TargetResolution;
import btk.staj.WorkFlowProject.workflow.model.WorkflowRecordSnapshot;
import btk.staj.WorkFlowProject.workflow.model.WorkflowUserSnapshot;
import btk.staj.WorkFlowProject.workflow.port.WorkflowUserPort;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import btk.staj.WorkFlowProject.workflow.statemachine.TargetStrategy;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

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
    private static final UUID DEPUTY_ID = UUID.fromString("00000000-0000-0000-0000-000000000008");
    private static final UUID SECOND_DEPUTY_ID = UUID.fromString("00000000-0000-0000-0000-000000000009");

    private final WorkflowUserPort userPort = mock(WorkflowUserPort.class);
    private final TargetUserResolver resolver = new TargetUserResolver(userPort);

    @Test
    void roleStrategyResolveTheSingleActiveDeputy() {
        WorkflowUserSnapshot deputy = user(DEPUTY_ID, RoleName.BASKAN_YARDIMCISI, true);
        when(userPort.findActiveByRole(WorkflowRoleFixtures.id(RoleName.BASKAN_YARDIMCISI))).thenReturn(List.of(deputy));

        TargetResolution result = resolver.resolve(TargetStrategy.ROLE, WorkflowRoleFixtures.id(RoleName.BASKAN_YARDIMCISI), null, record(LAST_DEPUTY_ID));

        assertThat(result).isEqualTo(new TargetResolution.Resolved(deputy));
    }

    @Test
    void roleStrategyReturnRoleNotConfiguredWhenNoActiveDeputyExists() {
        when(userPort.findActiveByRole(WorkflowRoleFixtures.id(RoleName.BASKAN_YARDIMCISI))).thenReturn(List.of());

        TargetResolution result = resolver.resolve(TargetStrategy.ROLE, WorkflowRoleFixtures.id(RoleName.BASKAN_YARDIMCISI), null, record(LAST_DEPUTY_ID));

        assertThat(result).isEqualTo(
                new TargetResolution.RoleNotConfigured(WorkflowRoleFixtures.id(RoleName.BASKAN_YARDIMCISI), 0));
    }

    @Test
    void roleStrategyReturnRoleNotConfiguredWhenMultipleActiveDeputiesExist() {
        WorkflowUserSnapshot first = user(DEPUTY_ID, RoleName.BASKAN_YARDIMCISI, true);
        WorkflowUserSnapshot second = user(SECOND_DEPUTY_ID, RoleName.BASKAN_YARDIMCISI, true);
        when(userPort.findActiveByRole(WorkflowRoleFixtures.id(RoleName.BASKAN_YARDIMCISI)))
                .thenReturn(List.of(first, second));

        TargetResolution result = resolver.resolve(TargetStrategy.ROLE, WorkflowRoleFixtures.id(RoleName.BASKAN_YARDIMCISI), null, record(LAST_DEPUTY_ID));

        assertThat(result).isEqualTo(
                new TargetResolution.RoleNotConfigured(WorkflowRoleFixtures.id(RoleName.BASKAN_YARDIMCISI), 2));
    }

    /**
     * Karar 4 geregi istekteki hedef reddedilir; bu kontrol servis/validator
     * isidir. Resolver acisindan onemli olan, degeri hic okumamasi: aksi halde
     * istemci hedefi sessizce gecerli olabilirdi.
     */
    @Test
    void roleStrategyIgnoreTheRequestTarget() {
        WorkflowUserSnapshot deputy = user(DEPUTY_ID, RoleName.BASKAN_YARDIMCISI, true);
        when(userPort.findActiveByRole(WorkflowRoleFixtures.id(RoleName.BASKAN_YARDIMCISI))).thenReturn(List.of(deputy));

        TargetResolution result = resolver.resolve(TargetStrategy.ROLE, WorkflowRoleFixtures.id(RoleName.BASKAN_YARDIMCISI), REQUESTED_TARGET_ID, record(LAST_DEPUTY_ID));

        assertThat(result).isEqualTo(new TargetResolution.Resolved(deputy));
        verify(userPort).findActiveByRole(WorkflowRoleFixtures.id(RoleName.BASKAN_YARDIMCISI));
        verifyNoMoreInteractions(userPort);
    }

    @Test
    void singleDeputyPortResultRemainsResolvedForValidatorWithoutRevalidation() {
        WorkflowUserSnapshot inconsistentSnapshot = user(DEPUTY_ID, RoleName.BASKAN, false);
        when(userPort.findActiveByRole(WorkflowRoleFixtures.id(RoleName.BASKAN_YARDIMCISI)))
                .thenReturn(List.of(inconsistentSnapshot));

        TargetResolution result = resolver.resolve(TargetStrategy.ROLE, WorkflowRoleFixtures.id(RoleName.BASKAN_YARDIMCISI), null, record(LAST_DEPUTY_ID));

        assertThat(result).isEqualTo(new TargetResolution.Resolved(inconsistentSnapshot));
    }

    @Test
    void baskanaIletResolvesTheSingleActivePresident() {
        WorkflowUserSnapshot president = user(PRESIDENT_ID, RoleName.BASKAN, true);
        when(userPort.findActiveByRole(WorkflowRoleFixtures.id(RoleName.BASKAN))).thenReturn(List.of(president));

        TargetResolution result = resolver.resolve(TargetStrategy.ROLE, WorkflowRoleFixtures.id(RoleName.BASKAN), REQUESTED_TARGET_ID, record(LAST_DEPUTY_ID));

        assertThat(result).isEqualTo(new TargetResolution.Resolved(president));
        verify(userPort).findActiveByRole(WorkflowRoleFixtures.id(RoleName.BASKAN));
        verifyNoMoreInteractions(userPort);
    }

    @Test
    void baskanaIletReturnsRoleNotConfiguredWhenNoActivePresidentExists() {
        when(userPort.findActiveByRole(WorkflowRoleFixtures.id(RoleName.BASKAN))).thenReturn(List.of());

        TargetResolution result = resolver.resolve(TargetStrategy.ROLE, WorkflowRoleFixtures.id(RoleName.BASKAN), null, record(LAST_DEPUTY_ID));

        assertThat(result).isEqualTo(new TargetResolution.RoleNotConfigured(WorkflowRoleFixtures.id(RoleName.BASKAN), 0));
    }

    @Test
    void baskanaIletReturnsRoleNotConfiguredWhenMultipleActivePresidentsExist() {
        WorkflowUserSnapshot first = user(PRESIDENT_ID, RoleName.BASKAN, true);
        WorkflowUserSnapshot second = user(SECOND_PRESIDENT_ID, RoleName.BASKAN, true);
        when(userPort.findActiveByRole(WorkflowRoleFixtures.id(RoleName.BASKAN))).thenReturn(List.of(first, second));

        TargetResolution result = resolver.resolve(TargetStrategy.ROLE, WorkflowRoleFixtures.id(RoleName.BASKAN), null, record(LAST_DEPUTY_ID));

        assertThat(result).isEqualTo(new TargetResolution.RoleNotConfigured(WorkflowRoleFixtures.id(RoleName.BASKAN), 2));
    }

    @Test
    void singlePresidentPortResultRemainsResolvedForValidatorWithoutRevalidation() {
        WorkflowUserSnapshot inconsistentSnapshot = user(PRESIDENT_ID, RoleName.ADMIN, false);
        when(userPort.findActiveByRole(WorkflowRoleFixtures.id(RoleName.BASKAN))).thenReturn(List.of(inconsistentSnapshot));

        TargetResolution result = resolver.resolve(TargetStrategy.ROLE, WorkflowRoleFixtures.id(RoleName.BASKAN), null, record(LAST_DEPUTY_ID));

        assertThat(result).isEqualTo(new TargetResolution.Resolved(inconsistentSnapshot));
    }

    @Test
    void calisanaGeriGonderResolvesCreatedByAndIgnoresRequestTarget() {
        WorkflowUserSnapshot creator = user(CREATOR_ID, RoleName.CALISAN, true);
        when(userPort.findById(CREATOR_ID)).thenReturn(Optional.of(creator));

        TargetResolution result = resolver.resolve(TargetStrategy.CREATOR, WorkflowRoleFixtures.id(RoleName.CALISAN), REQUESTED_TARGET_ID, record(LAST_DEPUTY_ID));

        assertThat(result).isEqualTo(new TargetResolution.Resolved(creator));
        verify(userPort).findById(CREATOR_ID);
        verifyNoMoreInteractions(userPort);
    }

    @Test
    void calisanaGeriGonderReturnsDataIntegrityFailureWhenCreatorUserIsMissing() {
        when(userPort.findById(CREATOR_ID)).thenReturn(Optional.empty());

        TargetResolution result = resolver.resolve(TargetStrategy.CREATOR, WorkflowRoleFixtures.id(RoleName.CALISAN), null, record(LAST_DEPUTY_ID));

        assertThat(result).isEqualTo(new TargetResolution.DataIntegrityFailure(
                DataIntegrityReason.CREATED_BY_USER_NOT_FOUND,
                CREATOR_ID));
    }

    @Test
    void baskanYardimcisinaGeriGonderReturnsDataIntegrityFailureWhenLastDeputyIdIsMissing() {
        TargetResolution result = resolver.resolve(TargetStrategy.PREVIOUS_ACTOR, WorkflowRoleFixtures.id(RoleName.BASKAN_YARDIMCISI), null, record(null));

        assertThat(result).isEqualTo(new TargetResolution.DataIntegrityFailure(
                DataIntegrityReason.LAST_DEPUTY_ID_MISSING,
                null));
        verifyNoInteractions(userPort);
    }

    @Test
    void baskanYardimcisinaGeriGonderReturnsDataIntegrityFailureWhenDeputyUserIsMissing() {
        when(userPort.findById(LAST_DEPUTY_ID)).thenReturn(Optional.empty());

        TargetResolution result = resolver.resolve(TargetStrategy.PREVIOUS_ACTOR, WorkflowRoleFixtures.id(RoleName.BASKAN_YARDIMCISI), null, record(LAST_DEPUTY_ID));

        assertThat(result).isEqualTo(new TargetResolution.DataIntegrityFailure(
                DataIntegrityReason.LAST_DEPUTY_USER_NOT_FOUND,
                LAST_DEPUTY_ID));
    }

    @Test
    void baskanYardimcisinaGeriGonderResolvesLastDeputy() {
        WorkflowUserSnapshot deputy = user(LAST_DEPUTY_ID, RoleName.BASKAN_YARDIMCISI, true);
        when(userPort.findById(LAST_DEPUTY_ID)).thenReturn(Optional.of(deputy));

        TargetResolution result = resolver.resolve(TargetStrategy.PREVIOUS_ACTOR, WorkflowRoleFixtures.id(RoleName.BASKAN_YARDIMCISI), REQUESTED_TARGET_ID, record(LAST_DEPUTY_ID));

        assertThat(result).isEqualTo(new TargetResolution.Resolved(deputy));
        verify(userPort).findById(LAST_DEPUTY_ID);
        verifyNoMoreInteractions(userPort);
    }

    @Test
    void noneStrategyReturnsNotProvidedWithoutLookingUpRequestTarget() {
        TargetResolution result = resolver.resolve(TargetStrategy.NONE, null, REQUESTED_TARGET_ID, record(LAST_DEPUTY_ID));

        assertThat(result).isEqualTo(new TargetResolution.NotProvided());
        verifyNoInteractions(userPort);
    }

    @Test
    void requiredInputsRejectNull() {
        assertThatNullPointerException()
                .isThrownBy(() -> new TargetUserResolver(null))
                .withMessage("userPort");
        assertThatNullPointerException()
                .isThrownBy(() -> resolver.resolve(null, null, null, record(LAST_DEPUTY_ID)))
                .withMessage("strategy");
        assertThatNullPointerException()
                .isThrownBy(() -> resolver.resolve(TargetStrategy.NONE, null, null, null))
                .withMessage("record");
        assertThatNullPointerException()
                .isThrownBy(() -> resolver.resolve(TargetStrategy.ROLE, null, null, record(LAST_DEPUTY_ID)))
                .withMessage("expectedTargetRoleId");
    }

    @Test
    void currentAssigneeStrategyResolvesTheRecordAssignee() {
        WorkflowUserSnapshot assignee = user(ASSIGNEE_ID, RoleName.BASKAN_YARDIMCISI, true);
        when(userPort.findById(ASSIGNEE_ID)).thenReturn(Optional.of(assignee));

        TargetResolution result = resolver.resolve(TargetStrategy.CURRENT_ASSIGNEE, null, REQUESTED_TARGET_ID, record(LAST_DEPUTY_ID));

        assertThat(result).isEqualTo(new TargetResolution.Resolved(assignee));
        verify(userPort).findById(ASSIGNEE_ID);
        verifyNoMoreInteractions(userPort);
    }

    @Test
    void currentAssigneeStrategyReportsMissingAssignee() {
        TargetResolution result = resolver.resolve(TargetStrategy.CURRENT_ASSIGNEE, null, null, recordWithoutAssignee());

        assertThat(result).isEqualTo(new TargetResolution.DataIntegrityFailure(
                TargetResolution.DataIntegrityReason.CURRENT_ASSIGNEE_MISSING, null));
        verifyNoInteractions(userPort);
    }

    @Test
    void currentAssigneeStrategyReportsUnknownAssignee() {
        when(userPort.findById(ASSIGNEE_ID)).thenReturn(Optional.empty());

        TargetResolution result = resolver.resolve(TargetStrategy.CURRENT_ASSIGNEE, null, null, record(LAST_DEPUTY_ID));

        assertThat(result).isEqualTo(new TargetResolution.DataIntegrityFailure(
                TargetResolution.DataIntegrityReason.CURRENT_ASSIGNEE_USER_NOT_FOUND, ASSIGNEE_ID));
    }

    /**
     * Ayni aksiyonun farkli gecislerde farkli hedefe gidebilmesi, bu refactor'un asil
     * sebebi. Resolver artik aksiyonu hic gormedigi icin ayni cagri farkli stratejilerle
     * farkli sonuc uretir.
     */
    @Test
    void sameActionCanResolveDifferentTargetsUnderDifferentStrategies() {
        WorkflowUserSnapshot creator = user(CREATOR_ID, RoleName.CALISAN, true);
        WorkflowUserSnapshot deputy = user(LAST_DEPUTY_ID, RoleName.BASKAN_YARDIMCISI, true);
        when(userPort.findById(CREATOR_ID)).thenReturn(Optional.of(creator));
        when(userPort.findById(LAST_DEPUTY_ID)).thenReturn(Optional.of(deputy));

        assertThat(resolver.resolve(TargetStrategy.CREATOR, WorkflowRoleFixtures.id(RoleName.CALISAN), null, record(LAST_DEPUTY_ID)))
                .isEqualTo(new TargetResolution.Resolved(creator));
        assertThat(resolver.resolve(TargetStrategy.PREVIOUS_ACTOR, WorkflowRoleFixtures.id(RoleName.BASKAN_YARDIMCISI), null, record(LAST_DEPUTY_ID)))
                .isEqualTo(new TargetResolution.Resolved(deputy));
    }

    private static WorkflowUserSnapshot user(UUID id, RoleName role, boolean active) {
        return new WorkflowUserSnapshot(id, WorkflowRoleFixtures.id(role), active);
    }

    private static WorkflowRecordSnapshot recordWithoutAssignee() {
        return new WorkflowRecordSnapshot(
                RECORD_ID, RecordStatus.TASLAK, CREATOR_ID, null, LAST_DEPUTY_ID, null, 3);
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
