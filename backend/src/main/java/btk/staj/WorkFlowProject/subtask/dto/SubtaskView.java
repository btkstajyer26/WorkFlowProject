package btk.staj.WorkFlowProject.subtask.dto;

import btk.staj.WorkFlowProject.subtask.model.SubtaskStatus;
import java.time.LocalDateTime;
import java.util.UUID;

/** Public view of one Subtask; parent approval settings deliberately live outside this type. */
public record SubtaskView(
        UUID id,
        UUID parentRecordId,
        String title,
        String description,
        UUID assignedTo,
        String assignedToName,
        SubtaskStatus status,
        String resolutionComment,
        LocalDateTime createdAt,
        LocalDateTime completedAt) {
}
