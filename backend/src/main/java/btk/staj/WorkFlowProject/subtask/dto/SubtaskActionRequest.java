package btk.staj.WorkFlowProject.subtask.dto;

import btk.staj.WorkFlowProject.subtask.model.SubtaskAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request contract for one lightweight Subtask state transition. */
public record SubtaskActionRequest(
        @NotNull SubtaskAction action,
        @Size(max = 2000) String comment) {
}
