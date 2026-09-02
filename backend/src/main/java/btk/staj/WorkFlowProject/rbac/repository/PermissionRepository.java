package btk.staj.WorkFlowProject.rbac.repository;

import btk.staj.WorkFlowProject.rbac.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Integer> {
    Optional<Permission> findByCode(String code);
    List<Permission> findAllByOrderByIdAsc();
}