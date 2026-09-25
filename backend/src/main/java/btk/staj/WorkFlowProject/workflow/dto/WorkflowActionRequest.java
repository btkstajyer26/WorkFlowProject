package btk.staj.WorkFlowProject.workflow.dto;

import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** HTTP request payload for applying one workflow action to a record. */
public record WorkflowActionRequest(
        @NotNull WorkflowAction action,
        UUID targetUserId,
        @Size(max = 2000) String comment,
        Integer targetDepartmentId) {
    public WorkflowActionRequest(WorkflowAction action, UUID targetUserId, String comment) {
        this(action, targetUserId, comment, null);
    }

    @AssertTrue(message = "targetUserId ve targetDepartmentId birlikte gönderilemez")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isTargetExclusive() {
        return targetUserId == null || targetDepartmentId == null;
    }
}
