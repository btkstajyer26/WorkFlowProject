package btk.staj.WorkFlowProject.workflow.entity;

import jakarta.persistence.*;
import btk.staj.WorkFlowProject.workflow.statemachine.ActorRequirement;

/**
 * TransitionRules.java'daki RULES listesinin (8 satir) veritabani karsiligi.
 * (from_status, action, role) uclusunun hangi to_status'a gectigini tanimlar.
 *
 * FK'ler Record.java'daki gibi duz Integer alan olarak tutulur, @ManyToOne
 * kurulmaz (projede bu ilişki turu kullanilmiyor). actor_requirement ise
 * Record.status'un RecordStatus enum'una baglanmasiyla ayni desende,
 * mevcut ActorRequirement enum'una @Enumerated ile baglidir.
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

    @Column(name = "role_id", nullable = false)
    private Integer roleId;

    @Column(name = "to_status_id", nullable = false)
    private Integer toStatusId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_requirement", nullable = false, length = 30)
    private ActorRequirement actorRequirement;

    @Column(name = "requires_comment", nullable = false)
    private boolean requiresComment;

    public WorkflowTransitionEntity() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getFromStatusId() { return fromStatusId; }
    public void setFromStatusId(Integer fromStatusId) { this.fromStatusId = fromStatusId; }

    public Integer getActionId() { return actionId; }
    public void setActionId(Integer actionId) { this.actionId = actionId; }

    public Integer getRoleId() { return roleId; }
    public void setRoleId(Integer roleId) { this.roleId = roleId; }

    public Integer getToStatusId() { return toStatusId; }
    public void setToStatusId(Integer toStatusId) { this.toStatusId = toStatusId; }

    public ActorRequirement getActorRequirement() { return actorRequirement; }
    public void setActorRequirement(ActorRequirement actorRequirement) { this.actorRequirement = actorRequirement; }

    public boolean isRequiresComment() { return requiresComment; }
    public void setRequiresComment(boolean requiresComment) { this.requiresComment = requiresComment; }
}