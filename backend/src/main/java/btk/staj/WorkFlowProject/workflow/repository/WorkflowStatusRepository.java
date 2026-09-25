package btk.staj.WorkFlowProject.workflow.repository;

import btk.staj.WorkFlowProject.workflow.entity.WorkflowStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowStatusRepository extends JpaRepository<WorkflowStatusEntity, Integer> {
    Optional<WorkflowStatusEntity> findByName(String name);
    List<WorkflowStatusEntity> findAllByOrderByDisplayOrderAsc();
}