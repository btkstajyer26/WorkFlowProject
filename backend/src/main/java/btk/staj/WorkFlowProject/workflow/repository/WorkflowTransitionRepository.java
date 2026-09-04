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

    /** Conservative use check, including department queues even after eligibility/routing revocation. */
    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM records r
                JOIN workflow_statuses s ON s.name = r.status
                WHERE r.deleted_at IS NULL AND s.id = :statusId AND s.is_terminal = false
                  AND r.status NOT IN ('ONAYLANDI', 'REDDEDILDI')
                  AND (EXISTS (SELECT 1 FROM users u WHERE u.role_id = :roleId AND (
                    (:requirement = 'CREATOR' AND u.id = r.created_by) OR
                    (:requirement = 'ASSIGNEE' AND u.id = r.assigned_to) OR
                    (:requirement = 'CREATOR_AND_ASSIGNEE'
                        AND u.id = r.created_by AND u.id = r.assigned_to)))
                  OR (r.assigned_department_id IS NOT NULL
                      AND :requirement IN ('ASSIGNEE', 'CREATOR_AND_ASSIGNEE')
                      AND EXISTS (SELECT 1 FROM department_routing_rules routing
                          WHERE routing.department_id = r.assigned_department_id
                            AND routing.from_status_id = s.id AND routing.target_role_id = :roleId)
                      AND (:requirement = 'ASSIGNEE' OR EXISTS (
                          SELECT 1 FROM users creator WHERE creator.id = r.created_by AND creator.role_id = :roleId)))))
            """, nativeQuery = true)
    boolean hasOpenRecords(@Param("statusId") Integer statusId, @Param("roleId") Integer roleId,
                           @Param("requirement") String requirement);

    // uq_transition_from_action_role kisitiyla birebir eslesir - V15 migration.
    Optional<WorkflowTransitionEntity> findByFromStatusIdAndActionIdAndActorRoleId(
            Integer fromStatusId, Integer actionId, Integer actorRoleId);

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
