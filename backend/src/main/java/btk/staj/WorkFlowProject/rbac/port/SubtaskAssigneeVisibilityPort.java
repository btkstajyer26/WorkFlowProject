package btk.staj.WorkFlowProject.rbac.port;

import java.util.UUID;

/** Lets a Subtask assignee view the Parent record their Subtask belongs to. */
public interface SubtaskAssigneeVisibilityPort {
    boolean isAssignedToAnySubtaskOf(UUID recordId, UUID actorId);
}
