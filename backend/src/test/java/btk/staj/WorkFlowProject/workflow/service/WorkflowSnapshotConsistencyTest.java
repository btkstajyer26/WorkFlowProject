package btk.staj.WorkFlowProject.workflow.service;

import btk.staj.WorkFlowProject.workflow.adapter.ReloadableTransitionRuleSource;
import btk.staj.WorkFlowProject.workflow.dto.WorkflowActionRequest;
import btk.staj.WorkFlowProject.workflow.exception.WorkflowApplicationException;
import btk.staj.WorkFlowProject.workflow.model.CurrentActor;
import btk.staj.WorkFlowProject.workflow.model.TargetResolution;
import btk.staj.WorkFlowProject.workflow.model.TransitionRuleRecord;
import btk.staj.WorkFlowProject.workflow.model.WorkflowRecordSnapshot;
import btk.staj.WorkFlowProject.workflow.model.WorkflowUserSnapshot;
import btk.staj.WorkFlowProject.workflow.port.AuditService;
import btk.staj.WorkFlowProject.workflow.port.WorkflowEventPublisher;
import btk.staj.WorkFlowProject.workflow.port.WorkflowRecordPort;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowErrorCode;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowTransitionValidator;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WorkflowSnapshotConsistencyTest {
    @Test
    void reloadBetweenValidationPassesDoesNotChangeAnInFlightAction() throws Exception {
        RoleId actorRole = new RoleId(71);
        RoleId targetRole = new RoleId(83);
        UUID actor = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        TransitionRuleRecord send = new TransitionRuleRecord("TASLAK", "GONDER", actorRole.value(),
                "CREATOR", "BSK_YRD_INCELEMESINDE", "ROLE", targetRole.value(), "RECORD_FORWARD");
        TransitionRuleRecord other = new TransitionRuleRecord("BASKAN_INCELEMESINDE", "ONAYLA", targetRole.value(),
                "ASSIGNEE", "ONAYLANDI", "NONE", null, "RECORD_APPROVE");
        AtomicReference<List<TransitionRuleRecord>> rows = new AtomicReference<>(List.of(send, other));
        ReloadableTransitionRuleSource source = new ReloadableTransitionRuleSource(rows::get);
        var originalSnapshot = source.snapshot();
        WorkflowRecordPort records = mock(WorkflowRecordPort.class);
        when(records.findById(recordId)).thenReturn(Optional.of(new WorkflowRecordSnapshot(
                recordId, RecordStatus.TASLAK, actor, null, null, null, 0)));
        TargetUserResolver resolver = mock(TargetUserResolver.class);
        CountDownLatch resolving = new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(1);
        when(resolver.resolve(any(), any(), any(), any())).thenAnswer(invocation -> {
            resolving.countDown();
            if (!finish.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("resolution timed out");
            return new TargetResolution.Resolved(new WorkflowUserSnapshot(target, targetRole, true));
        });
        WorkflowApplicationService service = new WorkflowApplicationService(records,
                () -> new CurrentActor(actor, actorRole, true, Set.of("RECORD_VIEW", "RECORD_FORWARD")),
                resolver, new DepartmentRoutingResolver(org.mockito.Mockito.mock(btk.staj.WorkFlowProject.workflow.port.DepartmentRoutingPort.class)), new WorkflowTransitionValidator(source), source,
                mock(AuditService.class), mock(WorkflowEventPublisher.class), Clock.systemUTC());
        WorkflowActionRequest request = new WorkflowActionRequest(WorkflowAction.GONDER, null, null);
        try (var executor = Executors.newSingleThreadExecutor()) {
            var inFlight = executor.submit(() -> service.performAction(recordId, request));
            try {
                assertThat(resolving.await(10, TimeUnit.SECONDS)).isTrue();
                rows.set(List.of(other));
                source.reload();
                assertThat(source.snapshot()).isNotSameAs(originalSnapshot);
                assertThat(originalSnapshot.find(RecordStatus.TASLAK, WorkflowAction.GONDER, actorRole)).isPresent();
                finish.countDown();
                assertThat(inFlight.get(10, TimeUnit.SECONDS).newStatus()).isEqualTo(RecordStatus.BSK_YRD_INCELEMESINDE);
                assertThatThrownBy(() -> service.performAction(recordId, request))
                        .isInstanceOfSatisfying(WorkflowApplicationException.class,
                                ex -> assertThat(ex.errorCode()).isEqualTo(WorkflowErrorCode.WORKFLOW_INVALID_TRANSITION));
                verify(records, times(1)).update(any());
                verify(resolver, times(1)).resolve(any(), any(), any(), any());
            } finally { finish.countDown(); }
        }
    }
}
