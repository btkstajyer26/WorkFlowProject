package btk.staj.WorkFlowProject.workflow.repository;

import btk.staj.WorkFlowProject.workflow.entity.WorkflowActionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowActionRepository extends JpaRepository<WorkflowActionEntity, Integer> {
    Optional<WorkflowActionEntity> findByName(String name);
    List<WorkflowActionEntity> findAllByOrderByIdAsc();
}