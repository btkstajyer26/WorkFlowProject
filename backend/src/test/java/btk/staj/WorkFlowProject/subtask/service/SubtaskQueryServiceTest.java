package btk.staj.WorkFlowProject.subtask.service;

import btk.staj.WorkFlowProject.auth.security.CurrentVisibilityActorProvider;
import btk.staj.WorkFlowProject.auth.security.VisibilityActor;
import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.rbac.SystemRoleKey;
import btk.staj.WorkFlowProject.rbac.service.RecordAccessPolicy;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.subtask.dto.SubtaskAssignableUsersResponse;
import btk.staj.WorkFlowProject.subtask.dto.SubtaskListResponse;
import btk.staj.WorkFlowProject.subtask.dto.SubtaskView;
import btk.staj.WorkFlowProject.subtask.entity.Subtask;
import btk.staj.WorkFlowProject.subtask.mapper.SubtaskViewMapper;
import btk.staj.WorkFlowProject.subtask.model.SubtaskApprovalPolicy;
import btk.staj.WorkFlowProject.subtask.model.SubtaskStatus;
import btk.staj.WorkFlowProject.subtask.repository.SubtaskRepository;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubtaskQueryServiceTest {

    private static final UUID PARENT_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final VisibilityActor ACTOR = new VisibilityActor(
            UUID.fromString("30000000-0000-0000-0000-000000000003"),
            new RoleId(22),
            Optional.empty(),
            Set.of());
    private static final VisibilityActor DEPUTY_ACTOR = new VisibilityActor(
            UUID.fromString("30000000-0000-0000-0000-000000000004"),
            new RoleId(23),
            Optional.of(SystemRoleKey.BASKAN_YARDIMCISI),
            Set.of());

    @Mock private RecordRepository records;
    @Mock private SubtaskRepository subtasks;
    @Mock private UserRepository users;
    @Mock private SubtaskViewMapper mapper;
    @Mock private RecordAccessPolicy recordAccessPolicy;
    @Mock private CurrentVisibilityActorProvider visibilityActorProvider;

    private SubtaskQueryService service;

    @BeforeEach
    void setUp() {
        service = new SubtaskQueryService(
                records,
                subtasks,
                users,
                mapper,
                recordAccessPolicy,
                visibilityActorProvider);
        org.mockito.Mockito.lenient().when(visibilityActorProvider.currentVisibilityActor()).thenReturn(ACTOR);
    }

    @Test
    void listShowsEverythingOnlyToBaskanYardimcisi() {
        Record parent = parent(SubtaskApprovalPolicy.MAJORITY, 2);
        org.mockito.Mockito.lenient().when(visibilityActorProvider.currentVisibilityActor()).thenReturn(DEPUTY_ACTOR);
        Subtask subtask = new Subtask();
        SubtaskView view = new SubtaskView(
                UUID.randomUUID(), PARENT_ID, "Alt görev", null, UUID.randomUUID(),
                "Ada Lovelace", SubtaskStatus.ISLEM, null, null, null);
        when(records.findByIdAndDeletedAtIsNull(PARENT_ID)).thenReturn(Optional.of(parent));
        when(subtasks.findAllByParentRecord_IdOrderByCreatedAtAscIdAsc(PARENT_ID))
                .thenReturn(List.of(subtask));
        when(mapper.toView(subtask)).thenReturn(view);

        SubtaskListResponse response = service.list(PARENT_ID);

        assertThat(response.approvalPolicy()).isEqualTo(SubtaskApprovalPolicy.MAJORITY);
        assertThat(response.requiredApprovals()).isEqualTo(2);
        assertThat(response.subtasks()).containsExactly(view);
        verify(recordAccessPolicy).assertCanView(DEPUTY_ACTOR, parent);
    }

    @Test
    void listRestrictsToOwnSubtaskForAnyoneOtherThanBaskanYardimcisi() {
        // Parent'i olusturmus olmak dahil - yalnizca rol Baskan Yardimcisi degilse
        // tam liste hic gosterilmez, olsa dahi kendi disindaki hicbir iliski yetmez.
        Record parent = parent(SubtaskApprovalPolicy.UNANIMOUS, 2);
        parent.setCreatedBy(ACTOR.id());
        Subtask own = new Subtask();
        Subtask sibling = new Subtask();
        SubtaskView ownView = new SubtaskView(
                UUID.randomUUID(), PARENT_ID, "Kendi görevim", null, ACTOR.id(),
                "Aktör Kullanıcı", SubtaskStatus.ISLEM, null, null, null);
        SubtaskView siblingView = new SubtaskView(
                UUID.randomUUID(), PARENT_ID, "Başkasının görevi", null, UUID.randomUUID(),
                "Diğer Kullanıcı", SubtaskStatus.DEGERLENDIRME, null, null, null);
        when(records.findByIdAndDeletedAtIsNull(PARENT_ID)).thenReturn(Optional.of(parent));
        when(subtasks.findAllByParentRecord_IdOrderByCreatedAtAscIdAsc(PARENT_ID))
                .thenReturn(List.of(own, sibling));
        when(mapper.toView(own)).thenReturn(ownView);
        when(mapper.toView(sibling)).thenReturn(siblingView);

        SubtaskListResponse response = service.list(PARENT_ID);

        assertThat(response.subtasks()).containsExactly(ownView);
        assertThat(response.approvalPolicy()).isNull();
        assertThat(response.requiredApprovals()).isNull();
    }

    @Test
    void returnsTheUnsplitContractWithoutLoadingChildren() {
        Record parent = parent(null, null);
        when(records.findByIdAndDeletedAtIsNull(PARENT_ID)).thenReturn(Optional.of(parent));

        SubtaskListResponse response = service.list(PARENT_ID);

        assertThat(response.approvalPolicy()).isNull();
        assertThat(response.requiredApprovals()).isNull();
        assertThat(response.subtasks()).isEmpty();
        verify(recordAccessPolicy).assertCanView(ACTOR, parent);
        verify(subtasks, never()).findAllByParentRecord_IdOrderByCreatedAtAscIdAsc(PARENT_ID);
    }

    @Test
    void assignableUsersChecksParentVisibilityAndMapsFullNames() {
        Record parent = parent(null, null);
        when(records.findByIdAndDeletedAtIsNull(PARENT_ID)).thenReturn(Optional.of(parent));
        User first = user("Ada", "Lovelace");
        User second = user("Grace", "Hopper");
        when(users.findAssignableSubtaskCandidates("SISTEM")).thenReturn(List.of(first, second));

        SubtaskAssignableUsersResponse response = service.assignableUsers(PARENT_ID);

        assertThat(response.users()).hasSize(2);
        assertThat(response.users().get(0).id()).isEqualTo(first.getId());
        assertThat(response.users().get(0).fullName()).isEqualTo("Ada Lovelace");
        assertThat(response.users().get(1).fullName()).isEqualTo("Grace Hopper");
        verify(recordAccessPolicy).assertCanView(ACTOR, parent);
    }

    @Test
    void assignableUsersFailsWhenParentIsMissing() {
        when(records.findByIdAndDeletedAtIsNull(PARENT_ID)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(
                btk.staj.WorkFlowProject.subtask.exception.SubtaskException.class,
                () -> service.assignableUsers(PARENT_ID));
    }

    private static User user(String firstName, String lastName) {
        Role role = new Role();
        role.setId(1);
        role.setSystemKey("BASKAN_YARDIMCISI");
        role.setActive(true);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setActive(true);
        user.setRole(role);
        return user;
    }

    private static Record parent(SubtaskApprovalPolicy policy, Integer requiredApprovals) {
        Record parent = new Record();
        parent.setId(PARENT_ID);
        parent.setSubtaskApprovalPolicy(policy);
        parent.setSubtaskRequiredApprovals(requiredApprovals);
        return parent;
    }
}
