package btk.staj.WorkFlowProject.subtask.service;

import btk.staj.WorkFlowProject.notification.entity.NotificationType;
import btk.staj.WorkFlowProject.notification.service.NotificationService;
import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.subtask.dto.SubtaskCreateItem;
import btk.staj.WorkFlowProject.subtask.dto.SubtaskSplitRequest;
import btk.staj.WorkFlowProject.subtask.dto.SubtaskSplitResponse;
import btk.staj.WorkFlowProject.subtask.dto.SubtaskView;
import btk.staj.WorkFlowProject.subtask.entity.Subtask;
import btk.staj.WorkFlowProject.subtask.exception.SubtaskException;
import btk.staj.WorkFlowProject.subtask.mapper.SubtaskViewMapper;
import btk.staj.WorkFlowProject.subtask.model.SubtaskApprovalPolicy;
import btk.staj.WorkFlowProject.subtask.model.SubtaskStatus;
import btk.staj.WorkFlowProject.subtask.repository.SubtaskRepository;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import btk.staj.WorkFlowProject.workflow.dto.WorkflowActionRequest;
import btk.staj.WorkFlowProject.workflow.model.CurrentActor;
import btk.staj.WorkFlowProject.workflow.port.CurrentActorProvider;
import btk.staj.WorkFlowProject.workflow.service.WorkflowActionService;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubtaskServiceTest {

    private static final UUID PARENT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID ACTOR_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final List<UUID> ASSIGNEE_IDS = List.of(
            UUID.fromString("30000000-0000-0000-0000-000000000001"),
            UUID.fromString("30000000-0000-0000-0000-000000000002"),
            UUID.fromString("30000000-0000-0000-0000-000000000003"),
            UUID.fromString("30000000-0000-0000-0000-000000000004"),
            UUID.fromString("30000000-0000-0000-0000-000000000005"));
    private static final CurrentActor ACTOR =
            new CurrentActor(ACTOR_ID, new RoleId(22), true, Set.of("RECORD_FORWARD"));

    @Mock
    private RecordRepository records;
    @Mock
    private SubtaskRepository subtasks;
    @Mock
    private UserRepository users;
    @Mock
    private CurrentActorProvider currentActorProvider;
    @Mock
    private WorkflowActionService workflowActionService;
    @Mock
    private NotificationService notificationService;

    private SubtaskService service;

    @BeforeEach
    void setUp() {
        service = new SubtaskService(
                records,
                subtasks,
                users,
                currentActorProvider,
                workflowActionService,
                new SubtaskViewMapper(),
                notificationService);
    }

    @ParameterizedTest
    @MethodSource("approvalThresholds")
    void splitsWithBackendCalculatedThresholdAndTheAuthenticatedCreator(
            SubtaskApprovalPolicy policy,
            int count,
            int expectedRequiredApprovals) {
        Record parent = arrangeValidSplit(policy, count);

        SubtaskSplitResponse response = service.split(PARENT_ID, request(policy, count));

        assertThat(response.parentRecordId()).isEqualTo(PARENT_ID);
        assertThat(response.approvalPolicy()).isEqualTo(policy);
        assertThat(response.requiredApprovals()).isEqualTo(expectedRequiredApprovals);
        assertThat(response.subtasks())
                .extracting(SubtaskView::title)
                .containsExactlyElementsOf(expectedTitles(count));
        assertThat(response.subtasks())
                .extracting(SubtaskView::status)
                .containsOnly(SubtaskStatus.DEGERLENDIRME);
        assertThat(parent.getSubtaskApprovalPolicy()).isEqualTo(policy);
        assertThat(parent.getSubtaskRequiredApprovals()).isEqualTo(expectedRequiredApprovals);

        List<Subtask> saved = capturedSubtasks();
        assertThat(saved).hasSize(count);
        assertThat(saved)
                .extracting(subtask -> subtask.getCreatedBy().getId())
                .containsOnly(ACTOR_ID);
        assertThat(saved).extracting(Subtask::getStatus).containsOnly(SubtaskStatus.DEGERLENDIRME);
        assertThat(saved).extracting(Subtask::getResolutionComment).containsOnlyNulls();
        assertThat(saved).extracting(Subtask::getCompletedAt).containsOnlyNulls();

        ArgumentCaptor<WorkflowActionRequest> action =
                ArgumentCaptor.forClass(WorkflowActionRequest.class);
        verify(workflowActionService).performAction(org.mockito.ArgumentMatchers.eq(PARENT_ID), action.capture());
        assertThat(action.getValue().action()).isEqualTo(WorkflowAction.ALT_GOREVLERE_AYIR);
        assertThat(action.getValue().targetUserId()).isNull();
        assertThat(action.getValue().targetDepartmentId()).isNull();

        ArgumentCaptor<UUID> notifiedUsers = ArgumentCaptor.forClass(UUID.class);
        verify(notificationService, times(count)).create(
                notifiedUsers.capture(),
                org.mockito.ArgumentMatchers.eq(PARENT_ID),
                any(),
                org.mockito.ArgumentMatchers.eq(NotificationType.SUBTASK_ASSIGNED));
        assertThat(notifiedUsers.getAllValues())
                .containsExactlyElementsOf(ASSIGNEE_IDS.subList(0, count));
    }

    @Test
    void rejectsAMissingParent() {
        when(records.findByIdAndDeletedAtIsNull(PARENT_ID)).thenReturn(Optional.empty());

        assertReason(
                () -> service.split(PARENT_ID, request(SubtaskApprovalPolicy.UNANIMOUS, 2)),
                SubtaskException.Reason.PARENT_NOT_FOUND);

        verifyNoInteractions(subtasks, users, currentActorProvider, workflowActionService);
    }

    @Test
    void rejectsAParentInTheWrongStatus() {
        Record parent = parent(RecordStatus.TASLAK);
        when(records.findByIdAndDeletedAtIsNull(PARENT_ID)).thenReturn(Optional.of(parent));
        when(subtasks.countByParentRecord_Id(PARENT_ID)).thenReturn(0L);

        assertReason(
                () -> service.split(PARENT_ID, request(SubtaskApprovalPolicy.UNANIMOUS, 2)),
                SubtaskException.Reason.PARENT_STATUS_INVALID);

        verifyNoInteractions(users, currentActorProvider, workflowActionService);
        verify(subtasks, never()).saveAll(any());
    }

    @Test
    void rejectsAParentThatAlreadyHasSubtasks() {
        Record parent = parent(RecordStatus.BSK_YRD_INCELEMESINDE);
        when(records.findByIdAndDeletedAtIsNull(PARENT_ID)).thenReturn(Optional.of(parent));
        when(subtasks.countByParentRecord_Id(PARENT_ID)).thenReturn(1L);

        assertReason(
                () -> service.split(PARENT_ID, request(SubtaskApprovalPolicy.UNANIMOUS, 2)),
                SubtaskException.Reason.ALREADY_SPLIT);

        verifyNoInteractions(users, currentActorProvider, workflowActionService);
        verify(subtasks, never()).saveAll(any());
    }

    @Test
    void rejectsFewerThanTwoSubtasksEvenWithoutBeanValidation() {
        arrangeParentWithoutSubtasks();

        assertReason(
                () -> service.split(PARENT_ID, request(SubtaskApprovalPolicy.UNANIMOUS, 1)),
                SubtaskException.Reason.COUNT_TOO_LOW);

        verifyNoInteractions(users, currentActorProvider, workflowActionService);
        verify(subtasks, never()).saveAll(any());
    }

    @Test
    void rejectsDuplicateAssigneesBeforeLoadingUsers() {
        arrangeParentWithoutSubtasks();
        UUID duplicate = ASSIGNEE_IDS.getFirst();
        SubtaskSplitRequest request = new SubtaskSplitRequest(
                SubtaskApprovalPolicy.UNANIMOUS,
                List.of(item(1, duplicate), item(2, duplicate)));

        assertReason(
                () -> service.split(PARENT_ID, request),
                SubtaskException.Reason.DUPLICATE_ASSIGNEE);

        verifyNoInteractions(users, currentActorProvider, workflowActionService);
        verify(subtasks, never()).saveAll(any());
    }

    @Test
    void rejectsAMissingAssignee() {
        arrangeParentWithoutSubtasks();
        List<SubtaskCreateItem> items = request(SubtaskApprovalPolicy.UNANIMOUS, 2).subtasks();
        when(users.findAllByIdIn(assigneeIds(items)))
                .thenReturn(List.of(assignee(ASSIGNEE_IDS.getFirst(), true, "CALISAN")));

        assertReason(
                () -> service.split(PARENT_ID, new SubtaskSplitRequest(
                        SubtaskApprovalPolicy.UNANIMOUS, items)),
                SubtaskException.Reason.ASSIGNEE_NOT_FOUND);

        verifyNoInteractions(currentActorProvider, workflowActionService);
        verify(subtasks, never()).saveAll(any());
    }

    @Test
    void rejectsAnInactiveAssignee() {
        arrangeParentWithoutSubtasks();
        List<SubtaskCreateItem> items = request(SubtaskApprovalPolicy.UNANIMOUS, 2).subtasks();
        when(users.findAllByIdIn(assigneeIds(items))).thenReturn(List.of(
                assignee(ASSIGNEE_IDS.getFirst(), false, "CALISAN"),
                assignee(ASSIGNEE_IDS.get(1), true, "CALISAN")));

        assertReason(
                () -> service.split(PARENT_ID, new SubtaskSplitRequest(
                        SubtaskApprovalPolicy.UNANIMOUS, items)),
                SubtaskException.Reason.ASSIGNEE_INACTIVE);

        verifyNoInteractions(currentActorProvider, workflowActionService);
        verify(subtasks, never()).saveAll(any());
    }

    @Test
    void rejectsAnyUserCarryingTheSystemActorRole() {
        arrangeParentWithoutSubtasks();
        List<SubtaskCreateItem> items = request(SubtaskApprovalPolicy.UNANIMOUS, 2).subtasks();
        when(users.findAllByIdIn(assigneeIds(items))).thenReturn(List.of(
                assignee(ASSIGNEE_IDS.getFirst(), true, "SISTEM"),
                assignee(ASSIGNEE_IDS.get(1), true, "CALISAN")));

        assertReason(
                () -> service.split(PARENT_ID, new SubtaskSplitRequest(
                        SubtaskApprovalPolicy.UNANIMOUS, items)),
                SubtaskException.Reason.ASSIGNEE_SYSTEM);

        verifyNoInteractions(currentActorProvider, workflowActionService);
        verify(subtasks, never()).saveAll(any());
    }

    @Test
    void rejectsANullApprovalPolicyEvenWithoutBeanValidation() {
        arrangeParentWithoutSubtasks();

        assertReason(
                () -> service.split(PARENT_ID, request(null, 2)),
                SubtaskException.Reason.INVALID_APPROVAL_POLICY);

        verifyNoInteractions(users, currentActorProvider, workflowActionService);
        verify(subtasks, never()).saveAll(any());
    }

    @Test
    void propagatesWorkflowFailureAfterPersistWasRequested() {
        arrangeValidSplit(SubtaskApprovalPolicy.UNANIMOUS, 2);
        RuntimeException failure = new RuntimeException("workflow failed");
        org.mockito.Mockito.doThrow(failure)
                .when(workflowActionService)
                .performAction(org.mockito.ArgumentMatchers.eq(PARENT_ID), any());

        assertThatThrownBy(() -> service.split(
                PARENT_ID,
                request(SubtaskApprovalPolicy.UNANIMOUS, 2)))
                .isSameAs(failure);

        verifyNoInteractions(notificationService);

        InOrder order = inOrder(subtasks, workflowActionService);
        order.verify(subtasks).saveAll(any());
        order.verify(workflowActionService).performAction(
                org.mockito.ArgumentMatchers.eq(PARENT_ID), any());
    }

    @Test
    void splitDefinesTheTransactionBoundary() throws Exception {
        Method split = SubtaskService.class.getMethod(
                "split", UUID.class, SubtaskSplitRequest.class);

        assertThat(split.getAnnotation(Transactional.class)).isNotNull();
    }

    private Record arrangeValidSplit(SubtaskApprovalPolicy policy, int count) {
        Record parent = arrangeParentWithoutSubtasks();
        List<SubtaskCreateItem> items = request(policy, count).subtasks();
        when(users.findAllByIdIn(assigneeIds(items))).thenReturn(IntStream.range(0, count)
                .mapToObj(index -> assignee(ASSIGNEE_IDS.get(index), true, "CALISAN"))
                .toList());
        when(currentActorProvider.currentActor()).thenReturn(ACTOR);
        when(users.findById(ACTOR_ID)).thenReturn(Optional.of(actorUser()));
        return parent;
    }

    private Record arrangeParentWithoutSubtasks() {
        Record parent = parent(RecordStatus.BSK_YRD_INCELEMESINDE);
        when(records.findByIdAndDeletedAtIsNull(PARENT_ID)).thenReturn(Optional.of(parent));
        when(subtasks.countByParentRecord_Id(PARENT_ID)).thenReturn(0L);
        return parent;
    }

    @SuppressWarnings("unchecked")
    private List<Subtask> capturedSubtasks() {
        ArgumentCaptor<Iterable<Subtask>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(subtasks).saveAll(captor.capture());
        return StreamSupport.stream(captor.getValue().spliterator(), false).toList();
    }

    private static SubtaskSplitRequest request(SubtaskApprovalPolicy policy, int count) {
        List<SubtaskCreateItem> items = IntStream.range(0, count)
                .mapToObj(index -> item(index + 1, ASSIGNEE_IDS.get(index)))
                .toList();
        return new SubtaskSplitRequest(policy, items);
    }

    private static List<String> expectedTitles(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(index -> "Alt görev " + index)
                .toList();
    }

    private static SubtaskCreateItem item(int number, UUID assigneeId) {
        return new SubtaskCreateItem("Alt görev " + number, "Açıklama " + number, assigneeId);
    }

    private static LinkedHashSet<UUID> assigneeIds(Collection<SubtaskCreateItem> items) {
        LinkedHashSet<UUID> ids = new LinkedHashSet<>();
        items.forEach(item -> ids.add(item.assignedTo()));
        return ids;
    }

    private static Record parent(RecordStatus status) {
        Record parent = new Record();
        parent.setId(PARENT_ID);
        parent.setStatus(status);
        parent.setVersion(0);
        return parent;
    }

    private static User actorUser() {
        User user = user(ACTOR_ID, true, "BASKAN_YARDIMCISI");
        user.setFirstName("Gerçek");
        user.setLastName("Aktör");
        return user;
    }

    private static User assignee(UUID id, boolean active, String systemKey) {
        User user = user(id, active, systemKey);
        int index = ASSIGNEE_IDS.indexOf(id) + 1;
        user.setFirstName("Kullanıcı");
        user.setLastName(String.valueOf(index));
        return user;
    }

    private static User user(UUID id, boolean active, String systemKey) {
        Role role = new Role();
        role.setId(Math.abs(systemKey.hashCode()) + 1);
        role.setName(systemKey);
        role.setSystem(true);
        role.setSystemKey(systemKey);
        role.setActive(true);

        User user = new User();
        user.setId(id);
        user.setActive(active);
        user.setRole(role);
        return user;
    }

    private static Stream<Arguments> approvalThresholds() {
        return java.util.stream.Stream.of(
                Arguments.of(SubtaskApprovalPolicy.UNANIMOUS, 2, 2),
                Arguments.of(SubtaskApprovalPolicy.UNANIMOUS, 4, 4),
                Arguments.of(SubtaskApprovalPolicy.MAJORITY, 2, 2),
                Arguments.of(SubtaskApprovalPolicy.MAJORITY, 3, 2),
                Arguments.of(SubtaskApprovalPolicy.MAJORITY, 4, 3),
                Arguments.of(SubtaskApprovalPolicy.MAJORITY, 5, 3));
    }

    private static void assertReason(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable invocation,
            SubtaskException.Reason expectedReason) {
        assertThatThrownBy(invocation)
                .isInstanceOfSatisfying(
                        SubtaskException.class,
                        exception -> assertThat(exception.reason()).isEqualTo(expectedReason));
    }
}
