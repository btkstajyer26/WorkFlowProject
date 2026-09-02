package btk.staj.WorkFlowProject.workflow.repository;

import btk.staj.WorkFlowProject.workflow.entity.WorkflowTransitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransitionEntity, Integer> {

    // uq_transition_from_action_role kisitiyla birebir eslesir - V15 migration.
    Optional<WorkflowTransitionEntity> findByFromStatusIdAndActionIdAndActorRoleId(
            Integer fromStatusId, Integer actionId, Integer actorRoleId);

    List<WorkflowTransitionEntity> findAllByFromStatusId(Integer fromStatusId);

    List<WorkflowTransitionEntity> findAllByActiveTrueOrderByIdAsc();
}