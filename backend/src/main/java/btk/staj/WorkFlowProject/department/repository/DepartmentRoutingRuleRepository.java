package btk.staj.WorkFlowProject.department.repository;

import btk.staj.WorkFlowProject.department.entity.DepartmentRoutingRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentRoutingRuleRepository extends JpaRepository<DepartmentRoutingRuleEntity, Integer> {

    // WF-6 DepartmentRoutingResolver'in ihtiyaci olan tam sorgu:
    // "bu departmanda, bu durumda, bu aksiyon icin hedef rol ne?"
    Optional<DepartmentRoutingRuleEntity> findByDepartmentIdAndFromStatusIdAndActionIdAndActiveTrue(
            Integer departmentId, Integer fromStatusId, Integer actionId);

    List<DepartmentRoutingRuleEntity> findAllByDepartmentIdOrderByIdAsc(Integer departmentId);
}