package btk.staj.WorkFlowProject.subtask.dto;

import btk.staj.WorkFlowProject.subtask.model.SubtaskApprovalPolicy;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Result contract returned after a Parent record is split. */
public record SubtaskSplitResponse(
        UUID parentRecordId,
        SubtaskApprovalPolicy approvalPolicy,
        Integer requiredApprovals,
        List<SubtaskView> subtasks) {

    public SubtaskSplitResponse {
        Objects.requireNonNull(parentRecordId, "parentRecordId");
        Objects.requireNonNull(approvalPolicy, "approvalPolicy");
        if (requiredApprovals == null || requiredApprovals < 1) {
            throw new IllegalArgumentException("requiredApprovals must be positive");
        }
        subtasks = List.copyOf(Objects.requireNonNull(subtasks, "subtasks"));
    }
}
