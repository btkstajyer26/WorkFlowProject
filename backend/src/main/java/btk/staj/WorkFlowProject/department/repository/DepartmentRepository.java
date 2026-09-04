package btk.staj.WorkFlowProject.department.repository;

import btk.staj.WorkFlowProject.department.entity.DepartmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<DepartmentEntity, Integer> {
    Optional<DepartmentEntity> findByName(String name);
    List<DepartmentEntity> findAllByOrderByNameAsc();
    List<DepartmentEntity> findAllByActiveTrueOrderByNameAsc();
    List<DepartmentEntity> findAllByParentDepartmentId(Integer parentDepartmentId);
}