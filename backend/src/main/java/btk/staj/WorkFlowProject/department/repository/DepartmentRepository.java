package btk.staj.WorkFlowProject.department.repository;

import btk.staj.WorkFlowProject.department.entity.DepartmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<DepartmentEntity, Integer> {
    Optional<DepartmentEntity> findByName(String name);
    List<DepartmentEntity> findAllByOrderByNameAsc();
    List<DepartmentEntity> findAllByActiveTrueOrderByNameAsc();
    List<DepartmentEntity> findAllByParentDepartmentId(Integer parentDepartmentId);

    // AP-4 yazicilari: RoleRepository/RecordRepository ile ayni kilit kalibi.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM DepartmentEntity d WHERE d.id = :id")
    Optional<DepartmentEntity> findByIdForUpdate(@Param("id") Integer id);
}