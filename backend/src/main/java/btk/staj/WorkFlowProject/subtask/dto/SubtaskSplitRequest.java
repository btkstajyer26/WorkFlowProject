package btk.staj.WorkFlowProject.subtask.dto;

import btk.staj.WorkFlowProject.subtask.model.SubtaskApprovalPolicy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Request contract for splitting a Parent into at least two Subtasks. */
public record SubtaskSplitRequest(
        @NotNull SubtaskApprovalPolicy approvalPolicy,
        @NotNull
        @Size(min = 2)
        List<@Valid SubtaskCreateItem> subtasks) {

    public SubtaskSplitRequest {
        subtasks = subtasks == null ? null : List.copyOf(subtasks);
    }
}
