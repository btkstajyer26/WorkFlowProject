package btk.staj.WorkFlowProject.rbac.repository;

import btk.staj.WorkFlowProject.rbac.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermission.Id> {
    List<RolePermission> findAllByIdRoleId(Integer roleId);
    List<RolePermission> findAllByIdPermissionId(Integer permissionId);
}