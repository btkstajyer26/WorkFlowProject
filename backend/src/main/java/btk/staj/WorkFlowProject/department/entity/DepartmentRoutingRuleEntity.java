package btk.staj.WorkFlowProject.department.entity;

import jakarta.persistence.*;

/**
 * Departmana atanmis bir kaydin, belirli (durum, aksiyon) icin hangi
 * roldeki uyenin islem yapabilecegini tanimlar.
 * bkz. WORKFLOW_V1_V2_PLANI.md SS11.
 *
 * TASLAK: Burak'in (WF-6) final routing semantigini onaylamasi
 * bekleniyor - kolon/kisit onaydan sonra degisebilir.
 *
 * FK'ler WorkflowTransitionEntity/Record.java konvansiyonuna uyarak duz
 * Integer alan olarak tutulur, @ManyToOne kurulmaz.
 */
@Entity
@Table(name = "department_routing_rules")
public class DepartmentRoutingRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "department_id", nullable = false)
    private Integer departmentId;

    @Column(name = "from_status_id", nullable = false)
    private Integer fromStatusId;

    @Column(name = "action_id", nullable = false)
    private Integer actionId;

    @Column(name = "target_role_id", nullable = false)
    private Integer targetRoleId;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    public DepartmentRoutingRuleEntity() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getDepartmentId() { return departmentId; }
    public void setDepartmentId(Integer departmentId) { this.departmentId = departmentId; }

    public Integer getFromStatusId() { return fromStatusId; }
    public void setFromStatusId(Integer fromStatusId) { this.fromStatusId = fromStatusId; }

    public Integer getActionId() { return actionId; }
    public void setActionId(Integer actionId) { this.actionId = actionId; }

    public Integer getTargetRoleId() { return targetRoleId; }
    public void setTargetRoleId(Integer targetRoleId) { this.targetRoleId = targetRoleId; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}