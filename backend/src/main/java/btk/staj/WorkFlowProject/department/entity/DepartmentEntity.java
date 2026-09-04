package btk.staj.WorkFlowProject.department.entity;

import jakarta.persistence.*;

/**
 * Kullanicilarin uye olabilecegi organizasyon grubu / is kuyrugu.
 * bkz. ADR-0005, WORKFLOW_V1_V2_PLANI.md SS10/SS14.
 *
 * parentDepartmentId, Record.java'daki FK konvansiyonuna uyarak duz
 * Integer alan olarak tutulur, @ManyToOne kurulmaz. V1'de hiyerarsi
 * yalniz yapisal bilgidir - otomatik eskalasyon runtime'da YOKTUR.
 */
@Entity
@Table(name = "departments")
public class DepartmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Column(name = "parent_department_id")
    private Integer parentDepartmentId;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    public DepartmentEntity() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getParentDepartmentId() { return parentDepartmentId; }
    public void setParentDepartmentId(Integer parentDepartmentId) { this.parentDepartmentId = parentDepartmentId; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
