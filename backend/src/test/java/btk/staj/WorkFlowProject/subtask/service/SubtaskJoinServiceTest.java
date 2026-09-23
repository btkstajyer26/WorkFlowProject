package btk.staj.WorkFlowProject.subtask.service;

import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.subtask.exception.SubtaskException;
import btk.staj.WorkFlowProject.subtask.model.SubtaskApprovalPolicy;
import btk.staj.WorkFlowProject.subtask.model.SubtaskStatus;
import btk.staj.WorkFlowProject.subtask.repository.SubtaskRepository;
import btk.staj.WorkFlowProject.subtask.repository.SubtaskStatusCount;
import btk.staj.WorkFlowProject.workflow.dto.WorkflowActionRequest;
import btk.staj.WorkFlowProject.workflow.model.CurrentActor;
import btk.staj.WorkFlowProject.workflow.port.SystemActorProvider;
import btk.staj.WorkFlowProject.workflow.service.WorkflowApplicationService;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubtaskJoinServiceTest {

    private static final UUID PARENT_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID SYSTEM_ID = UUID.fromString("90000000-0000-0000-0000-000000000009");
    private static final CurrentActor SYSTEM_ACTOR =
            new CurrentActor(SYSTEM_ID, new RoleId(99), true, Set.of());

    @Mock
    private RecordRepository records;
    @Mock
    private SubtaskRepository subtasks;
    @Mock
    private SystemActorProvider systemActorProvider;
    @Mock
    private WorkflowApplicationService workflowApplicationService;

    private SubtaskJoinService service;

    @BeforeEach
    void setUp() {
        service = new SubtaskJoinService(
                records,
                subtasks,
                systemActorProvider,
                workflowApplicationService);
    }

    @Test
    void locksParentBeforeRecountAndWaitsWhileAnySiblingIsNonTerminal() {
        arrangeParent(parent(
                RecordStatus.ALT_GOREV_BEKLIYOR,
                SubtaskApprovalPolicy.UNANIMOUS,
                2));
        when(subtasks.countStatusesByParentRecordId(PARENT_ID)).thenReturn(List.of(
                count(SubtaskStatus.TAMAMLANDI, 1),
                count(SubtaskStatus.ONAY, 1)));

        assertThat(service.joinIfReady(PARENT_ID))
                .isEqualTo(SubtaskJoinService.JoinOutcome.WAITING_FOR_SUBTASKS);

        InOrder order = inOrder(records, subtasks);
        order.verify(records).findByIdForUpdate(PARENT_ID);
        order.verify(subtasks).countStatusesByParentRecordId(PARENT_ID);
        verifyNoInteractions(systemActorProvider, workflowApplicationService);
    }

    @Test
    void allTerminalSubtasksUseTheTrustedSystemActorAndExplicitWorkflowOverload() {
        arrangeParent(parent(
                RecordStatus.ALT_GOREV_BEKLIYOR,
                SubtaskApprovalPolicy.UNANIMOUS,
                2));
        when(subtasks.countStatusesByParentRecordId(PARENT_ID)).thenReturn(List.of(
                count(SubtaskStatus.TAMAMLANDI, 2)));
        when(systemActorProvider.systemActor()).thenReturn(SYSTEM_ACTOR);

        assertThat(service.joinIfReady(PARENT_ID))
                .isEqualTo(SubtaskJoinService.JoinOutcome.TRANSITIONED_APPROVED);

        ArgumentCaptor<WorkflowActionRequest> request =
                ArgumentCaptor.forClass(WorkflowActionRequest.class);
        verify(systemActorProvider).systemActor();
        verify(workflowApplicationService).performAction(
                eq(PARENT_ID),
                request.capture(),
                eq(SYSTEM_ACTOR));
        assertThat(request.getValue().action())
                .isEqualTo(WorkflowAction.ALT_GOREVLER_SONUCLANDI);
        assertThat(request.getValue().targetUserId()).isNull();
        assertThat(request.getValue().targetDepartmentId()).isNull();
        assertThat(request.getValue().comment()).isNull();
    }

    @Test
    void unanimousRejectionResultStillTransitionsAfterEverySubtaskIsTerminal() {
        arrangeParent(parent(
                RecordStatus.ALT_GOREV_BEKLIYOR,
                SubtaskApprovalPolicy.UNANIMOUS,
                2));
        when(subtasks.countStatusesByParentRecordId(PARENT_ID)).thenReturn(List.of(
                count(SubtaskStatus.TAMAMLANDI, 1),
                count(SubtaskStatus.REDDEDILDI, 1)));
        when(systemActorProvider.systemActor()).thenReturn(SYSTEM_ACTOR);

        assertThat(service.joinIfReady(PARENT_ID))
                .isEqualTo(SubtaskJoinService.JoinOutcome.TRANSITIONED_REJECTED);

        verify(workflowApplicationService).performAction(
                eq(PARENT_ID),
                org.mockito.ArgumentMatchers.any(WorkflowActionRequest.class),
                eq(SYSTEM_ACTOR));
    }

    @Test
    void majorityUsesTheStoredFloorHalfPlusOneThreshold() {
        arrangeParent(parent(
                RecordStatus.ALT_GOREV_BEKLIYOR,
                SubtaskApprovalPolicy.MAJORITY,
                2));
        when(subtasks.countStatusesByParentRecordId(PARENT_ID)).thenReturn(List.of(
                count(SubtaskStatus.TAMAMLANDI, 2),
                count(SubtaskStatus.REDDEDILDI, 1)));
        when(systemActorProvider.systemActor()).thenReturn(SYSTEM_ACTOR);

        assertThat(service.joinIfReady(PARENT_ID))
                .isEqualTo(SubtaskJoinService.JoinOutcome.TRANSITIONED_APPROVED);

        verify(workflowApplicationService).performAction(
                eq(PARENT_ID),
                org.mockito.ArgumentMatchers.any(WorkflowActionRequest.class),
                eq(SYSTEM_ACTOR));
    }

    @Test
    void majorityBelowThresholdStillTransitionsWithARejectedResult() {
        arrangeParent(parent(
                RecordStatus.ALT_GOREV_BEKLIYOR,
                SubtaskApprovalPolicy.MAJORITY,
                2));
        when(subtasks.countStatusesByParentRecordId(PARENT_ID)).thenReturn(List.of(
                count(SubtaskStatus.TAMAMLANDI, 1),
                count(SubtaskStatus.REDDEDILDI, 2)));
        when(systemActorProvider.systemActor()).thenReturn(SYSTEM_ACTOR);

        assertThat(service.joinIfReady(PARENT_ID))
                .isEqualTo(SubtaskJoinService.JoinOutcome.TRANSITIONED_REJECTED);

        verify(workflowApplicationService).performAction(
                eq(PARENT_ID),
                org.mockito.ArgumentMatchers.any(WorkflowActionRequest.class),
                eq(SYSTEM_ACTOR));
    }

    @Test
    void alreadyJoinedParentIsAnIdempotentNoOp() {
        arrangeParent(parent(
                RecordStatus.KONTROL,
                SubtaskApprovalPolicy.UNANIMOUS,
                2));

        assertThat(service.joinIfReady(PARENT_ID))
                .isEqualTo(SubtaskJoinService.JoinOutcome.ALREADY_JOINED);

        verifyNoInteractions(subtasks, systemActorProvider, workflowApplicationService);
    }

    @Test
    void rejectsAParentInAnUnrelatedWorkflowState() {
        arrangeParent(parent(
                RecordStatus.BSK_YRD_INCELEMESINDE,
                SubtaskApprovalPolicy.UNANIMOUS,
                2));

        assertReason(
                () -> service.joinIfReady(PARENT_ID),
                SubtaskException.Reason.PARENT_STATUS_INVALID);

        verifyNoInteractions(subtasks, systemActorProvider, workflowApplicationService);
    }

    @Test
    void rejectsMissingApprovalPolicyWithoutCallingTheWorkflow() {
        arrangeParent(parent(RecordStatus.ALT_GOREV_BEKLIYOR, null, 2));
        when(subtasks.countStatusesByParentRecordId(PARENT_ID)).thenReturn(List.of(
                count(SubtaskStatus.TAMAMLANDI, 2)));

        assertReason(
                () -> service.joinIfReady(PARENT_ID),
                SubtaskException.Reason.INVALID_APPROVAL_POLICY);

        verifyNoInteractions(systemActorProvider, workflowApplicationService);
    }

    @Test
    void rejectsAStoredThresholdThatDoesNotMatchThePolicyAndSiblingCount() {
        arrangeParent(parent(
                RecordStatus.ALT_GOREV_BEKLIYOR,
                SubtaskApprovalPolicy.MAJORITY,
                3));
        when(subtasks.countStatusesByParentRecordId(PARENT_ID)).thenReturn(List.of(
                count(SubtaskStatus.TAMAMLANDI, 2),
                count(SubtaskStatus.REDDEDILDI, 1)));

        assertReason(
                () -> service.joinIfReady(PARENT_ID),
                SubtaskException.Reason.INVALID_REQUIRED_APPROVALS);

        verifyNoInteractions(systemActorProvider, workflowApplicationService);
    }

    @Test
    void rejectsAnInvalidParentWithFewerThanTwoSubtasks() {
        arrangeParent(parent(
                RecordStatus.ALT_GOREV_BEKLIYOR,
                SubtaskApprovalPolicy.UNANIMOUS,
                1));
        when(subtasks.countStatusesByParentRecordId(PARENT_ID)).thenReturn(List.of(
                count(SubtaskStatus.TAMAMLANDI, 1)));

        assertReason(
                () -> service.joinIfReady(PARENT_ID),
                SubtaskException.Reason.INVALID_SUBTASK);

        verifyNoInteractions(systemActorProvider, workflowApplicationService);
    }

    @Test
    void joinDefinesATransactionBoundary() throws Exception {
        Method method = SubtaskJoinService.class.getMethod("joinIfReady", UUID.class);

        assertThat(method.getAnnotation(Transactional.class)).isNotNull();
    }

    private void arrangeParent(Record parent) {
        when(records.findByIdForUpdate(PARENT_ID)).thenReturn(Optional.of(parent));
    }

    private static Record parent(
            RecordStatus status,
            SubtaskApprovalPolicy policy,
            Integer requiredApprovals) {
        Record parent = new Record();
        parent.setId(PARENT_ID);
        parent.setStatus(status);
        parent.setSubtaskApprovalPolicy(policy);
        parent.setSubtaskRequiredApprovals(requiredApprovals);
        return parent;
    }

    private static SubtaskStatusCount count(SubtaskStatus status, long count) {
        return new SubtaskStatusCount(status, count);
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
