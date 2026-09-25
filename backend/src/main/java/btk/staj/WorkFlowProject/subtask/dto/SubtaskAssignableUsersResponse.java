package btk.staj.WorkFlowProject.subtask.dto;

import java.util.List;
import java.util.Objects;

/** Response contract for GET /api/records/{recordId}/subtasks/assignable-users. */
public record SubtaskAssignableUsersResponse(List<SubtaskAssignableUserView> users) {

    public SubtaskAssignableUsersResponse {
        users = List.copyOf(Objects.requireNonNull(users, "users"));
    }
}
