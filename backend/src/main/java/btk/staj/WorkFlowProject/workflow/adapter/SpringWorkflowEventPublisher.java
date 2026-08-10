package btk.staj.WorkFlowProject.workflow.adapter;

import btk.staj.WorkFlowProject.workflow.model.WorkflowStatusChangedEvent;
import btk.staj.WorkFlowProject.workflow.port.WorkflowEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Objects;

/** Publishes workflow state changes through Spring's application event mechanism. */
@Component
public final class SpringWorkflowEventPublisher implements WorkflowEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringWorkflowEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = Objects.requireNonNull(
                applicationEventPublisher,
                "applicationEventPublisher");
    }

    @Override
    public void publish(WorkflowStatusChangedEvent event) {
        applicationEventPublisher.publishEvent(Objects.requireNonNull(event, "event"));
    }
}
