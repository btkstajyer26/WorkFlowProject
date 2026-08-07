package btk.staj.WorkFlowProject.workflow.port;

import btk.staj.WorkFlowProject.workflow.model.WorkflowStatusChangedEvent;

/** Publishes successful workflow state changes to downstream integrations. */
public interface WorkflowEventPublisher {

    void publish(WorkflowStatusChangedEvent event);
}
