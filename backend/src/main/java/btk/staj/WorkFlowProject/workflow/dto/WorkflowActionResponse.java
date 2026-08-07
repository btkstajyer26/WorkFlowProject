package btk.staj.WorkFlowProject.workflow.dto;

import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;

import java.time.Instant;
import java.util.UUID;

/** Backend-calculated result of a successful workflow transition. */
public record WorkflowActionResponse(
        UUID recordId,
        WorkflowAction action,
        RecordStatus previousStatus,
        RecordStatus newStatus,
        UUID assignedTo,
        UUID performedBy,
        Instant performedAt) {
}
