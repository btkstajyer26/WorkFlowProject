package btk.staj.WorkFlowProject.workflow.port;

import btk.staj.WorkFlowProject.workflow.model.WorkflowUserSnapshot;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Boundary through which the workflow core resolves users and role holders. */
public interface WorkflowUserPort {

    Optional<WorkflowUserSnapshot> findById(UUID userId);

    List<WorkflowUserSnapshot> findActiveByRole(RoleName role);
}
