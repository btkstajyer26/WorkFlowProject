package btk.staj.WorkFlowProject.workflow.adapter;

import btk.staj.WorkFlowProject.workflow.exception.TransitionRuleConfigurationException;
import btk.staj.WorkFlowProject.workflow.model.TransitionRuleRecord;
import btk.staj.WorkFlowProject.workflow.port.TransitionRuleRecordReader;
import btk.staj.WorkFlowProject.workflow.repository.TransitionRuleRow;
import btk.staj.WorkFlowProject.workflow.repository.WorkflowTransitionRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Reads role foreign keys without depending on role names or system keys. */
@Component
public final class JpaTransitionRuleRecordReader implements TransitionRuleRecordReader {
    private final WorkflowTransitionRepository transitionRepository;

    public JpaTransitionRuleRecordReader(WorkflowTransitionRepository transitionRepository) {
        this.transitionRepository = Objects.requireNonNull(transitionRepository, "transitionRepository");
    }

    @Override
    public List<TransitionRuleRecord> findAllActive() {
        List<TransitionRuleRow> rows = transitionRepository.findActiveRuleRows();
        if (rows == null) {
            throw new IllegalStateException("WorkflowTransitionRepository.findActiveRuleRows() returned null");
        }
        List<TransitionRuleRecord> records = new ArrayList<>(rows.size());
        for (int index = 0; index < rows.size(); index++) {
            records.add(toRecord(rows.get(index), index + 1));
        }
        return List.copyOf(records);
    }

    private static TransitionRuleRecord toRecord(TransitionRuleRow row, int rowNumber) {
        if (row == null) {
            throw new IllegalStateException("WorkflowTransitionRepository returned a null row at row " + rowNumber);
        }
        if (row.actorRequirement() == null) {
            throw new TransitionRuleConfigurationException(
                    "Transition configuration row " + rowNumber + " has a null actor_requirement");
        }
        return new TransitionRuleRecord(row.fromStatus(), row.action(), row.actorRoleId(),
                row.actorRequirement().name(), row.toStatus(), row.targetStrategy(),
                row.expectedTargetRoleId(), row.requiredPermissionCode());
    }
}
