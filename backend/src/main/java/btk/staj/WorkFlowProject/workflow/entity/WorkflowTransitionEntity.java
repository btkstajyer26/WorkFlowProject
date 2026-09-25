package btk.staj.WorkFlowProject.workflow.entity;

import jakarta.persistence.*;
import btk.staj.WorkFlowProject.workflow.statemachine.ActorRequirement;

/**
 * (from_status, action, actor_role) uclusunun hangi to_status'a gectigini,
 * hedef kullanicinin nasil cozulecegini ve gerekli permission'i tanimlar.
 * bkz. DB_1_VERI_MODELI_SOZLESMESI.md SS6.6.
 *
 * FK'ler Record.java'daki gibi duz Integer alan olarak tutulur, @ManyToOne
 * kurulmaz. actor_requirement mevcut ActorRequirement enum'una @Enumerated
 * ile baglidir (Record.status'un RecordStatus'a baglanmasiyla ayni desen).
 *
 * target_strategy icin henuz eslesen bir Java enum'u yok (bu kavram
 * sozlesmeyle birlikte yeni geldi) - simdilik duz String; WF kulvari
 * (SM-7/SM-8) bir TargetStrategy enum'u tanimlarsa buraya da @Enumerated
 * eklenebilir.
 *
 * Isim gecicidir, port imzasi toplantisinda kesinlesecek.
 */
@Entity
@Table(name = "workflow_transitions")
public class WorkflowTransitionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "from_status_id", nullable = false)
    private Integer fromStatusId;

    @Column(name = "action_id", nullable = false)
    private Integer actionId;

    @Column(name = "actor_role_id", nullable = false)
    private Integer actorRoleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_requirement", nullable = false, length = 40)
    private ActorRequirement actorRequirement;

    @Column(name = "to_status_id", nullable = false)
    private Integer toStatusId;

    @Column(name = "expected_target_role_id")
    private Integer expectedTargetRoleId;

    @Column(name = "target_strategy", nullable = false, length = 40)
    private String targetStrategy;

    @Column(name = "required_permission_id")
    private Integer requiredPermissionId;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    public WorkflowTransitionEntity() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getFromStatusId() { return fromStatusId; }
    public void setFromStatusId(Integer fromStatusId) { this.fromStatusId = fromStatusId; }

    public Integer getActionId() { return actionId; }
    public void setActionId(Integer actionId) { this.actionId = actionId; }

    public Integer getActorRoleId() { return actorRoleId; }
    public void setActorRoleId(Integer actorRoleId) { this.actorRoleId = actorRoleId; }

    public ActorRequirement getActorRequirement() { return actorRequirement; }
    public void setActorRequirement(ActorRequirement actorRequirement) { this.actorRequirement = actorRequirement; }

    public Integer getToStatusId() { return toStatusId; }
    public void setToStatusId(Integer toStatusId) { this.toStatusId = toStatusId; }

    public Integer getExpectedTargetRoleId() { return expectedTargetRoleId; }
    public void setExpectedTargetRoleId(Integer expectedTargetRoleId) { this.expectedTargetRoleId = expectedTargetRoleId; }

    public String getTargetStrategy() { return targetStrategy; }
    public void setTargetStrategy(String targetStrategy) { this.targetStrategy = targetStrategy; }

    public Integer getRequiredPermissionId() { return requiredPermissionId; }
    public void setRequiredPermissionId(Integer requiredPermissionId) { this.requiredPermissionId = requiredPermissionId; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}