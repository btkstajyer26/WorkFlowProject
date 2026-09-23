package btk.staj.WorkFlowProject.subtask.mapper;

import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.subtask.dto.SubtaskView;
import btk.staj.WorkFlowProject.subtask.entity.Subtask;
import btk.staj.WorkFlowProject.subtask.model.SubtaskStatus;
import btk.staj.WorkFlowProject.user.entity.User;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubtaskViewMapperTest {

    private final SubtaskViewMapper mapper = new SubtaskViewMapper();

    @Test
    void mapsThePublicFieldsFromTheLoadedEntityGraph() {
        UUID subtaskId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 22, 10, 15);
        LocalDateTime completedAt = LocalDateTime.of(2026, 9, 23, 11, 30);

        Record parent = new Record();
        parent.setId(parentId);
        User assignee = new User();
        assignee.setId(assigneeId);
        assignee.setFirstName("Ada");
        assignee.setLastName("Lovelace");
        Subtask subtask = Subtask.builder()
                .id(subtaskId)
                .parentRecord(parent)
                .title("Teknik inceleme")
                .description("Detay")
                .assignedTo(assignee)
                .status(SubtaskStatus.TAMAMLANDI)
                .resolutionComment("Uygun")
                .createdAt(createdAt)
                .completedAt(completedAt)
                .build();

        SubtaskView view = mapper.toView(subtask);

        assertThat(view).isEqualTo(new SubtaskView(
                subtaskId,
                parentId,
                "Teknik inceleme",
                "Detay",
                assigneeId,
                "Ada Lovelace",
                SubtaskStatus.TAMAMLANDI,
                "Uygun",
                createdAt,
                completedAt));
    }

    @Test
    void rejectsANullEntity() {
        assertThatThrownBy(() -> mapper.toView(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("subtask");
    }
}
