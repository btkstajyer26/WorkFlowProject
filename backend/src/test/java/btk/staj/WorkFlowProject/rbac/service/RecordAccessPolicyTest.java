package btk.staj.WorkFlowProject.rbac.service;

import btk.staj.WorkFlowProject.auth.security.VisibilityActor;
import btk.staj.WorkFlowProject.rbac.SystemRoleKey;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class RecordAccessPolicyTest {
    private final UUID viewer = UUID.randomUUID();
    private final UUID other = UUID.randomUUID();
    private final RecordAccessPolicy policy = new RecordAccessPolicy();

    private VisibilityActor actor(SystemRoleKey key, boolean permission) {
        return new VisibilityActor(viewer, new RoleId(101), Optional.ofNullable(key),
                permission ? Set.of("RECORD_VIEW") : Set.of());
    }

    private Record record(UUID creator, UUID assignee, UUID deputy, RecordStatus status) {
        return Record.builder().createdBy(creator).assignedTo(assignee).lastDeputyId(deputy).status(status).build();
    }

    @ParameterizedTest
    @EnumSource(RecordStatus.class)
    void dynamicReadersNeedOwnershipOrCurrentAssignmentInEveryStatus(RecordStatus status) {
        var dynamic = actor(null, true);
        assertThat(policy.canView(dynamic, record(viewer, null, null, status))).isTrue();
        assertThat(policy.canView(dynamic, record(other, viewer, null, status))).isTrue();
        assertThat(policy.canView(dynamic, record(other, other, viewer, status))).isFalse();
        assertThat(policy.canView(dynamic, record(other, null, null, status))).isFalse();
        assertThat(policy.seesHistoryFromPresidentHandover(dynamic)).isFalse();
        assertThat(policy.seesRecordAsOfHandoff(dynamic, other, status)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(SystemRoleKey.class)
    void permissionIsRequiredAndAdminIsAlwaysDenied(SystemRoleKey key) {
        var ownedAndAssigned = record(viewer, viewer, viewer, RecordStatus.ONAYLANDI);
        assertThat(policy.canView(actor(key, false), ownedAndAssigned)).isFalse();
        assertThat(policy.canView(actor(key, true), ownedAndAssigned)).isEqualTo(key != SystemRoleKey.ADMIN);
    }

    @Test
    void historicalSystemScopesAndClippingRemainExplicit() {
        var deputy = actor(SystemRoleKey.BASKAN_YARDIMCISI, true);
        var president = actor(SystemRoleKey.BASKAN, true);
        assertThat(policy.canView(deputy, record(other, other, null, RecordStatus.DUZENLEME_BEKLIYOR))).isTrue();
        assertThat(policy.canView(deputy, record(other, other, viewer, RecordStatus.REDDEDILDI))).isTrue();
        assertThat(policy.canView(deputy, record(other, null, null, RecordStatus.TASLAK))).isFalse();
        for (var status : Set.of(RecordStatus.BASKAN_INCELEMESINDE, RecordStatus.ONAYLANDI, RecordStatus.REDDEDILDI)) {
            assertThat(policy.canView(president, record(other, null, null, status))).isTrue();
        }
        assertThat(policy.canView(president, record(other, other, null, RecordStatus.DUZENLEME_BEKLIYOR))).isFalse();
        assertThat(policy.seesRecordAsOfHandoff(deputy, other, RecordStatus.DUZENLEME_BEKLIYOR)).isTrue();
        assertThat(policy.seesRecordAsOfHandoff(deputy, viewer, RecordStatus.DUZENLEME_BEKLIYOR)).isFalse();
        assertThat(policy.seesHistoryFromPresidentHandover(president)).isTrue();
    }

    @Test
    void deletedRecordsAreNeverVisibleIncludingToTheirCreator() {
        var deleted = record(viewer, viewer, viewer, RecordStatus.ONAYLANDI);
        deleted.setDeletedAt(LocalDateTime.now());
        for (var key : SystemRoleKey.values()) assertThat(policy.canView(actor(key, true), deleted)).isFalse();
        assertThat(policy.canView(actor(null, true), deleted)).isFalse();
    }
}
