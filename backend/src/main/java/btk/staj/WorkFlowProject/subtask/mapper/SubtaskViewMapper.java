package btk.staj.WorkFlowProject.subtask.mapper;

import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.subtask.dto.SubtaskView;
import btk.staj.WorkFlowProject.subtask.entity.Subtask;
import btk.staj.WorkFlowProject.user.entity.User;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Maps an already-loaded Subtask aggregate to its public child view. */
@Component
public final class SubtaskViewMapper {

    public SubtaskView toView(Subtask subtask) {
        Subtask source = Objects.requireNonNull(subtask, "subtask");
        Record parent = Objects.requireNonNull(source.getParentRecord(), "subtask.parentRecord");
        User assignee = Objects.requireNonNull(source.getAssignedTo(), "subtask.assignedTo");

        return new SubtaskView(
                source.getId(),
                parent.getId(),
                source.getTitle(),
                source.getDescription(),
                assignee.getId(),
                fullName(assignee),
                source.getStatus(),
                source.getResolutionComment(),
                source.getCreatedAt(),
                source.getCompletedAt());
    }

    private static String fullName(User user) {
        return (user.getFirstName() + " " + user.getLastName()).trim();
    }
}
