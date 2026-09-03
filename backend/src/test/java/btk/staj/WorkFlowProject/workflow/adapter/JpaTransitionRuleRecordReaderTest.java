package btk.staj.WorkFlowProject.workflow.adapter;

import btk.staj.WorkFlowProject.workflow.exception.TransitionRuleConfigurationException;
import btk.staj.WorkFlowProject.workflow.model.TransitionRuleRecord;
import btk.staj.WorkFlowProject.workflow.repository.TransitionRuleRow;
import btk.staj.WorkFlowProject.workflow.repository.WorkflowTransitionRepository;
import btk.staj.WorkFlowProject.workflow.statemachine.ActorRequirement;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class JpaTransitionRuleRecordReaderTest {
    @Test void mapsRoleForeignKeysAndMetadataWithoutNameConversion() {
        assertThat(readerReturning(row(1001, 2002)).findAllActive()).containsExactly(
                new TransitionRuleRecord(
                        "TASLAK",
                        "GONDER",
                        1001,
                        "CREATOR",
                        "BSK_YRD_INCELEMESINDE",
                        "ROLE",
                        2002,
                        "RECORD_FORWARD"));
    }

    @Test void preservesDifferentEnvironmentIdentities() {
        assertThat(readerReturning(row(701, 903)).findAllActive()).singleElement()
                .extracting(TransitionRuleRecord::actorRoleId, TransitionRuleRecord::expectedTargetRoleId)
                .containsExactly(701, 903);
    }

    @Test void representsDynamicActorWithItsForeignKey() {
        assertThat(readerReturning(row(5501, 2002)).findAllActive()).singleElement()
                .extracting(TransitionRuleRecord::actorRoleId).isEqualTo(5501);
    }

    @Test void representsDynamicTargetWithItsForeignKey() {
        assertThat(readerReturning(row(1001, 5502)).findAllActive()).singleElement()
                .extracting(TransitionRuleRecord::expectedTargetRoleId).isEqualTo(5502);
    }

    @Test void preservesRowOrder() {
        assertThat(readerReturning(row(1001, 2002), row(3003, 4004)).findAllActive())
                .extracting(TransitionRuleRecord::actorRoleId).containsExactly(1001, 3003);
    }

    @Test void leavesTargetAbsentForTerminalTransition() {
        var row = new TransitionRuleRow("BASKAN_INCELEMESINDE", "ONAYLA", 3003,
                ActorRequirement.ASSIGNEE, "ONAYLANDI", "NONE", null, "RECORD_APPROVE");
        assertThat(readerReturning(row).findAllActive()).singleElement()
                .extracting(TransitionRuleRecord::expectedTargetRoleId).isNull();
    }

    @Test void reportsNullRequirementWithItsRowNumber() {
        var invalid = new TransitionRuleRow("TASLAK", "GONDER", 1001, null,
                "BSK_YRD_INCELEMESINDE", "ROLE", 2002, "RECORD_FORWARD");
        assertThatThrownBy(() -> readerReturning(row(1001, 2002), invalid).findAllActive())
                .isInstanceOf(TransitionRuleConfigurationException.class)
                .hasMessageContaining("actor_requirement").hasMessageContaining("row 2");
    }

    @Test void leavesMissingActorIdentityForSourceValidation() {
        assertThat(readerReturning(row(null, 2002)).findAllActive()).singleElement()
                .extracting(TransitionRuleRecord::actorRoleId).isNull();
    }

    @Test void returnsEmptyRowsWithoutDecidingSourceValidity() {
        assertThat(readerReturning().findAllActive()).isEmpty();
    }

    @Test void rejectsNullRepositoryResult() {
        var repository = mock(WorkflowTransitionRepository.class);
        when(repository.findActiveRuleRows()).thenReturn(null);
        assertThatIllegalStateException().isThrownBy(() -> new JpaTransitionRuleRecordReader(repository).findAllActive())
                .withMessageContaining("findActiveRuleRows");
    }

    @Test void rejectsNullRepository() {
        assertThatNullPointerException().isThrownBy(() -> new JpaTransitionRuleRecordReader(null))
                .withMessageContaining("transitionRepository");
    }

    @Test void reportsNullRowPosition() {
        assertThatIllegalStateException().isThrownBy(() -> readerReturning(row(1001, 2002), null).findAllActive())
                .withMessageContaining("row 2");
    }

    @Test void returnsAnImmutableDetachedResult() {
        var repository = mock(WorkflowTransitionRepository.class);
        var rows = new ArrayList<>(List.of(row(1001, 2002)));
        when(repository.findActiveRuleRows()).thenReturn(rows);
        var result = new JpaTransitionRuleRecordReader(repository).findAllActive();
        rows.clear();
        assertThat(result).hasSize(1);
        assertThatThrownBy(() -> result.clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    private static JpaTransitionRuleRecordReader readerReturning(TransitionRuleRow... rows) {
        var repository = mock(WorkflowTransitionRepository.class);
        when(repository.findActiveRuleRows()).thenReturn(Arrays.asList(rows));
        return new JpaTransitionRuleRecordReader(repository);
    }

    private static TransitionRuleRow row(Integer actorRoleId, Integer targetRoleId) {
        return new TransitionRuleRow("TASLAK", "GONDER", actorRoleId, ActorRequirement.CREATOR,
                "BSK_YRD_INCELEMESINDE", "ROLE", targetRoleId, "RECORD_FORWARD");
    }
}
