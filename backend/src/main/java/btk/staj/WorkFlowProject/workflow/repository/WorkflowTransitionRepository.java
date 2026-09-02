package btk.staj.WorkFlowProject.workflow.repository;

import btk.staj.WorkFlowProject.workflow.entity.WorkflowTransitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransitionEntity, Integer> {

    // uq_transition_from_action_role kisitiyla birebir eslesir - V15 migration.
    Optional<WorkflowTransitionEntity> findByFromStatusIdAndActionIdAndActorRoleId(
            Integer fromStatusId, Integer actionId, Integer actorRoleId);

    List<WorkflowTransitionEntity> findAllByFromStatusId(Integer fromStatusId);

    List<WorkflowTransitionEntity> findAllByActiveTrueOrderByIdAsc();

    /**
     * Aktif gecisleri, sayisal FK'ler yerine teknik adlariyla dondurur.
     *
     * <p>Entity ile katalog tablolari arasinda JPA iliskisi yok (FK'ler duz
     * {@code Integer}), bu yuzden join'ler acik {@code ON} ile kurulur; ayni
     * desen {@code AuditLogRepository#findHistoryByRecordId} icinde de var.
     *
     * <p>Aktor rolu <strong>{@code system_key}</strong> uzerinden okunur.
     * {@code roles.name} yonetim panelinden degistirilebildigi icin kural
     * kimligi olarak kullanilamaz (DB-1 SS4); {@code r.name} yalnizca hata
     * mesajinda gorunmek uzere tasinir.
     *
     * <p>Filtre yalnizca {@code workflow_transitions.is_active} uzerindedir:
     * portun sozlesmesi ({@code findAllActive}) budur. Katalog satirlarinin
     * aktifligi DB-1 SS14 publish dogrulamasinin konusudur.
     *
     * <p>Beklenen hedef rol join'i <strong>{@code LEFT JOIN}</strong> olmak zorundadir:
     * {@code expected_target_role_id} hedef gerektirmeyen gecislerde bostur ve normal bir
     * join {@code ONAYLA} ile {@code REDDET} satirlarini sessizce dusururdu.
     */
    @Query("""
            SELECT new btk.staj.WorkFlowProject.workflow.repository.TransitionRuleRow(
                       fs.name, a.name, r.systemKey, r.name, t.actorRequirement, ts.name,
                       t.targetStrategy, t.expectedTargetRoleId, tr.systemKey)
            FROM WorkflowTransitionEntity t
            JOIN WorkflowStatusEntity fs ON fs.id = t.fromStatusId
            JOIN WorkflowActionEntity  a ON  a.id = t.actionId
            JOIN Role                  r ON  r.id = t.actorRoleId
            JOIN WorkflowStatusEntity ts ON ts.id = t.toStatusId
            LEFT JOIN Role            tr ON tr.id = t.expectedTargetRoleId
            WHERE t.active = true
            ORDER BY t.id ASC
            """)
    List<TransitionRuleRow> findActiveRuleRows();
}