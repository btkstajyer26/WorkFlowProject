package btk.staj.WorkFlowProject.rbac.repository;

import btk.staj.WorkFlowProject.rbac.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermission.Id> {
    @Query("""
            SELECT p.code FROM RolePermission rp
            JOIN Permission p ON p.id = rp.id.permissionId
            JOIN Role r ON r.id = rp.id.roleId
            WHERE r.id = :roleId AND r.active = true AND p.active = true
            ORDER BY p.code
            """)
    List<String> findActiveCodesByRoleId(@Param("roleId") Integer roleId);

    List<RolePermission> findAllByIdRoleId(Integer roleId);
    List<RolePermission> findAllByIdPermissionId(Integer permissionId);
}
