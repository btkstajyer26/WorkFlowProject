package btk.staj.WorkFlowProject.subtask.adapter;

import btk.staj.WorkFlowProject.rbac.port.SubtaskAssigneeVisibilityPort;
import btk.staj.WorkFlowProject.subtask.repository.SubtaskRepository;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SubtaskAssigneeVisibilityAdapter implements SubtaskAssigneeVisibilityPort {

    private final SubtaskRepository subtasks;

    public SubtaskAssigneeVisibilityAdapter(SubtaskRepository subtasks) {
        this.subtasks = Objects.requireNonNull(subtasks, "subtasks");
    }

    @Override
    public boolean isAssignedToAnySubtaskOf(UUID recordId, UUID actorId) {
        return subtasks.existsByParentRecord_IdAndAssignedTo_Id(recordId, actorId);
    }
}
