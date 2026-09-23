package btk.staj.WorkFlowProject.user.repository;

import btk.staj.WorkFlowProject.user.entity.User;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    @EntityGraph(attributePaths = "role")
    Optional<User> findByEmail(String email);
    @EntityGraph(attributePaths = "role")
    List<User> findAllByRole_SystemKey(String systemKey);
    @EntityGraph(attributePaths = "role")
    List<User> findAllByIdIn(Collection<UUID> ids);
    List<User> findByRole_SystemKeyAndRole_ActiveTrueAndActiveTrue(String systemKey);
    List<User> findByRole_IdAndRole_ActiveTrueAndActiveTrue(Integer roleId);
    long countByRole_IdAndActiveTrue(Integer roleId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") UUID id);

    /**
     * Active users eligible to be assigned a Subtask - every active role except
     * the non-human SISTEM role. {@code Role.systemKey} is null for dynamic
     * roles, so the exclusion is spelled out explicitly rather than relying on
     * a derived "Not" query, which would silently drop null-systemKey rows
     * under SQL's three-valued logic.
     */
    @EntityGraph(attributePaths = "role")
    @Query("""
            SELECT u FROM User u
            WHERE u.active = true
              AND u.role.active = true
              AND (u.role.systemKey IS NULL OR u.role.systemKey <> :excludedSystemKey)
            ORDER BY u.firstName ASC, u.lastName ASC
            """)
    List<User> findAssignableSubtaskCandidates(@Param("excludedSystemKey") String excludedSystemKey);
}
