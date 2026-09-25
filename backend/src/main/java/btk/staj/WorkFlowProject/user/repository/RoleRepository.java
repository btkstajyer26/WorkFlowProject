package btk.staj.WorkFlowProject.user.repository;

import btk.staj.WorkFlowProject.rbac.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(String name);
    Optional<Role> findBySystemKey(String systemKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Role r WHERE r.id = :id")
    Optional<Role> findByIdForUpdate(@Param("id") Integer id);
    List<Role> findAllByOrderByIdAsc();
}
