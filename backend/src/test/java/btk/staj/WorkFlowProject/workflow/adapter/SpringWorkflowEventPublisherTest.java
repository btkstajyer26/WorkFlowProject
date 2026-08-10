package btk.staj.WorkFlowProject.workflow.adapter;

import btk.staj.WorkFlowProject.workflow.model.WorkflowStatusChangedEvent;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class SpringWorkflowEventPublisherTest {

    private final ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
    private final SpringWorkflowEventPublisher publisher =
            new SpringWorkflowEventPublisher(applicationEventPublisher);

    @Test
    void publishForwardsTheSameEventExactlyOnce() {
        WorkflowStatusChangedEvent event = event();

        publisher.publish(event);

        verify(applicationEventPublisher, times(1)).publishEvent(same(event));
        verifyNoMoreInteractions(applicationEventPublisher);
    }

    @Test
    void requiredInputsRejectNull() {
        assertThatNullPointerException()
                .isThrownBy(() -> new SpringWorkflowEventPublisher(null))
                .withMessage("applicationEventPublisher");
        assertThatNullPointerException()
                .isThrownBy(() -> publisher.publish(null))
                .withMessage("event");
        verifyNoInteractions(applicationEventPublisher);
    }

    private static WorkflowStatusChangedEvent event() {
        return new WorkflowStatusChangedEvent(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                WorkflowAction.GONDER,
                RecordStatus.TASLAK,
                RecordStatus.BSK_YRD_INCELEMESINDE,
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                RoleName.CALISAN,
                null,
                UUID.fromString("00000000-0000-0000-0000-000000000003"),
                null,
                Instant.parse("2026-08-10T09:30:00Z"));
    }
}
