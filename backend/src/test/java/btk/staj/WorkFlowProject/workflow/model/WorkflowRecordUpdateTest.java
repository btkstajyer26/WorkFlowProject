package btk.staj.WorkFlowProject.workflow.model;

import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowRecordUpdateTest {
    @Test
    void userAndDepartmentCannotBeAssignedTogetherBeforePersistence() {
        assertThatThrownBy(() -> new WorkflowRecordUpdate(UUID.randomUUID(), RecordStatus.BSK_YRD_INCELEMESINDE,
                UUID.randomUUID(), null, 0, Instant.now(), 42)).isInstanceOf(IllegalArgumentException.class);
    }
}
