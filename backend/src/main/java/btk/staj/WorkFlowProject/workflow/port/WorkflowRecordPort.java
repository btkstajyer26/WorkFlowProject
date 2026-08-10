package btk.staj.WorkFlowProject.workflow.port;

import btk.staj.WorkFlowProject.workflow.model.WorkflowRecordSnapshot;
import btk.staj.WorkFlowProject.workflow.model.WorkflowRecordUpdate;

import java.util.Optional;
import java.util.UUID;

/** Boundary through which the workflow core reads and updates records. */
public interface WorkflowRecordPort {

    Optional<WorkflowRecordSnapshot> findById(UUID recordId);

    void update(WorkflowRecordUpdate update);
}
