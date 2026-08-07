package btk.staj.WorkFlowProject.workflow.port;

import btk.staj.WorkFlowProject.workflow.model.WorkflowTransitionAudit;

/** Records semantic audit data for a successful workflow transition. */
public interface AuditService {

    void record(WorkflowTransitionAudit audit);
}
