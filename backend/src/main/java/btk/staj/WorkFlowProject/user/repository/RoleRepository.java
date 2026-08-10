package btk.staj.WorkFlowProject.user.repository;

import btk.staj.WorkFlowProject.rbac.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(String name);
}