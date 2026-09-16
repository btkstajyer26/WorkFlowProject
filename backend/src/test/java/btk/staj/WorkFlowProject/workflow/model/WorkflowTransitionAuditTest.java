package btk.staj.WorkFlowProject.workflow.model;

import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * B12 / ADR-0009: atamanin her iki yani da kisiye VEYA departmana isaret
 * edebilir, ikisine birden degil. Ayni invariant DB tarafinda
 * chk_audit_previous_assignment_exclusive / chk_audit_new_assignment_exclusive
 * ile de zorlanir; buradaki testler modelin persistence'a hic ulasmadan
 * durdurdugunu sabitler (kalip: {@link WorkflowRecordUpdateTest}).
 */
class WorkflowTransitionAuditTest {

    private static WorkflowTransitionAudit audit(UUID previousAssignedTo,
                                                 Integer previousAssignedDepartmentId,
                                                 UUID newAssignedTo,
                                                 Integer newAssignedDepartmentId) {
        return new WorkflowTransitionAudit(
                UUID.randomUUID(),
                WorkflowAction.DEPARTMANA_GONDER,
                RecordStatus.TASLAK,
                RecordStatus.BSK_YRD_INCELEMESINDE,
                UUID.randomUUID(),
                new RoleId(1),
                previousAssignedTo,
                previousAssignedDepartmentId,
                newAssignedTo,
                newAssignedDepartmentId,
                null,
                Instant.now());
    }

    @Test
    void previousSideCannotHoldAUserAndADepartmentTogether() {
        assertThatThrownBy(() -> audit(UUID.randomUUID(), 42, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Previous");
    }

    @Test
    void newSideCannotHoldAUserAndADepartmentTogether() {
        assertThatThrownBy(() -> audit(null, null, UUID.randomUUID(), 42))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("New");
    }

    @Test
    void bothSidesMayBeEmpty() {
        // Terminal gecis ve TASLAK: atamasizlik gecerli bir durumdur (NONE).
        WorkflowTransitionAudit terminal = audit(null, null, null, null);
        assertThat(terminal.previousAssignedTo()).isNull();
        assertThat(terminal.newAssignedDepartmentId()).isNull();
    }

    @Test
    void aUserSideAndADepartmentSideMayCoexistOnOppositeSides() {
        // Kisiden departmana gonderim: yasak olan ayni yanda iki atama, karsilikli
        // yanlarda farkli turler degil.
        UUID previous = UUID.randomUUID();
        WorkflowTransitionAudit handover = audit(previous, null, null, 42);
        assertThat(handover.previousAssignedTo()).isEqualTo(previous);
        assertThat(handover.newAssignedDepartmentId()).isEqualTo(42);
    }
}
