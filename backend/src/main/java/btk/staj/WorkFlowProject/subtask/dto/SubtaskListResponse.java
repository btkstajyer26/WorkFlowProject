package btk.staj.WorkFlowProject.subtask.dto;

import btk.staj.WorkFlowProject.subtask.model.SubtaskApprovalPolicy;
import java.util.List;
import java.util.Objects;

/** Response contract for GET /api/records/{recordId}/subtasks. */
public record SubtaskListResponse(
        SubtaskApprovalPolicy approvalPolicy,
        Integer requiredApprovals,
        List<SubtaskView> subtasks) {

    public SubtaskListResponse {
        if ((approvalPolicy == null) != (requiredApprovals == null)) {
            throw new IllegalArgumentException(
                    "approvalPolicy and requiredApprovals must both be null or both be present");
        }
        if (requiredApprovals != null && requiredApprovals < 1) {
            throw new IllegalArgumentException("requiredApprovals must be positive");
        }
        subtasks = List.copyOf(Objects.requireNonNull(subtasks, "subtasks"));
    }

    public static SubtaskListResponse unsplit() {
        return new SubtaskListResponse(null, null, List.of());
    }
}
