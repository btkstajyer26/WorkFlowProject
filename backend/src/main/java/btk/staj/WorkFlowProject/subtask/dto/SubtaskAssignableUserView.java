package btk.staj.WorkFlowProject.subtask.dto;

import java.util.UUID;

/** One candidate a Parent record's split screen may assign a Subtask to. */
public record SubtaskAssignableUserView(UUID id, String fullName) {
}
