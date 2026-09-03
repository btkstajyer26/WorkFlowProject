package btk.staj.WorkFlowProject.workflow.repository;

import btk.staj.WorkFlowProject.workflow.entity.WorkflowTransitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransitionEntity, Integer> {

    // uq_transition_from_action_role kisitiyla birebir eslesir - V15 migration.
    Optional<WorkflowTransitionEntity> findByFromStatusIdAndActionIdAndActorRoleId(
            Integer fromStatusId, Integer actionId, Integer actorRoleId);

    List<WorkflowTransitionEntity> findAllByFromStatusId(Integer fromStatusId);

    List<WorkflowTransitionEntity> findAllByActiveTrueOrderByIdAsc();

    /** Active transitions with raw role foreign keys; names are display data only. */
    @Query("""
            SELECT new btk.staj.WorkFlowProject.workflow.repository.TransitionRuleRow(
                       fs.name, a.name, t.actorRoleId, t.actorRequirement, ts.name,
                       t.targetStrategy, t.expectedTargetRoleId, p.code)
            FROM WorkflowTransitionEntity t
            JOIN WorkflowStatusEntity fs ON fs.id = t.fromStatusId
            JOIN WorkflowActionEntity  a ON  a.id = t.actionId
            JOIN WorkflowStatusEntity ts ON ts.id = t.toStatusId
            LEFT JOIN Permission       p ON p.id = t.requiredPermissionId
            WHERE t.active = true
            ORDER BY t.id ASC
            """)
    List<TransitionRuleRow> findActiveRuleRows();
}
