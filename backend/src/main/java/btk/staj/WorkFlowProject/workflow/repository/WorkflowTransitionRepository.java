package btk.staj.WorkFlowProject.workflow.repository;

import btk.staj.WorkFlowProject.workflow.entity.WorkflowTransitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransitionEntity, Integer> {

    List<WorkflowTransitionEntity> findAllByOrderByIdAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM WorkflowTransitionEntity t WHERE t.id IN :ids ORDER BY t.id")
    List<WorkflowTransitionEntity> findAllForUpdate(@Param("ids") List<Integer> ids);

    /** Conservative use check: temporary user/role/permission revocation cannot bypass it. */
    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM records r
                JOIN workflow_statuses s ON s.name = r.status
                JOIN users u ON u.role_id = :roleId AND (
                    (:requirement = 'CREATOR' AND u.id = r.created_by) OR
                    (:requirement = 'ASSIGNEE' AND u.id = r.assigned_to) OR
                    (:requirement = 'CREATOR_AND_ASSIGNEE'
                        AND u.id = r.created_by AND u.id = r.assigned_to))
                WHERE r.deleted_at IS NULL AND s.id = :statusId AND s.is_terminal = false
                  AND r.status NOT IN ('ONAYLANDI', 'REDDEDILDI'))
            """, nativeQuery = true)
    boolean hasOpenRecords(@Param("statusId") Integer statusId, @Param("roleId") Integer roleId,
                           @Param("requirement") String requirement);

    // uq_transition_from_action_role kisitiyla birebir eslesir - V15 migration.
    Optional<WorkflowTransitionEntity> findByFromStatusIdAndActionIdAndActorRoleId(
            Integer fromStatusId, Integer actionId, Integer actorRoleId);

    /**
     * Rolun aktor oldugu aktif gecisler. AP-2'nin rol yazicilari, rolu pasiflestirmeden
     * veya aktorlugunu kapatmadan once bu baglarin her biri icin
     * {@link #hasOpenRecords} etki analizini yapar (DB-1 SS "uygulama servisi etki
     * analizi yapmadan ... izin vermemelidir").
     */
    List<WorkflowTransitionEntity> findAllByActorRoleIdAndActiveTrue(Integer actorRoleId);

    List<WorkflowTransitionEntity> findAllByFromStatusId(Integer fromStatusId);

    List<WorkflowTransitionEntity> findAllByActiveTrueOrderByIdAsc();

    /** Active transitions with raw role foreign keys; names are display data only. */
    @Query("""
            SELECT new btk.staj.WorkFlowProject.workflow.repository.TransitionRuleRow(
                       fs.name, a.name, t.actorRoleId, t.actorRequirement, ts.name,
                       t.targetStrategy, t.expectedTargetRoleId, p.code)
            FROM WorkflowTransitionEntity t
            JOIN WorkflowStatusEntity fs ON fs.id = t.fromStatusId
            JOIN WorkflowActionEntity  a ON  a.id = t.actionId
            JOIN WorkflowStatusEntity ts ON ts.id = t.toStatusId
            LEFT JOIN Permission       p ON p.id = t.requiredPermissionId
            WHERE t.active = true
            ORDER BY t.id ASC
            """)
    List<TransitionRuleRow> findActiveRuleRows();
}
