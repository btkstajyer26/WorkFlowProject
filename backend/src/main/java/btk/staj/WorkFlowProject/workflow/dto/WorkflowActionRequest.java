package btk.staj.WorkFlowProject.workflow.dto;

import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** HTTP request payload for applying one workflow action to a record. */
public record WorkflowActionRequest(
        @NotNull WorkflowAction action,
        UUID targetUserId,
        @Size(max = 2000) String comment) {
}
