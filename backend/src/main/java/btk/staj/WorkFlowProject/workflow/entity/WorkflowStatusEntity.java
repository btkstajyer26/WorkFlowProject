package btk.staj.WorkFlowProject.workflow.entity;

import jakarta.persistence.*;

/**
 * records.status kolonundaki degerlerin veritabani karsiligi.
 * bkz. DB_1_VERI_MODELI_SOZLESMESI.md SS6.4.
 *
 * Isim gecicidir: workflow.statemachine paketindeki RecordStatus enum'u ile
 * kavramsal cakisma var, port imzasi toplantisinda kesinlesecek.
 */
@Entity
@Table(name = "workflow_statuses")
public class WorkflowStatusEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "is_terminal", nullable = false)
    private boolean terminal;

    @Column(name = "is_editable_by_creator", nullable = false)
    private boolean editableByCreator;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    public WorkflowStatusEntity() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public boolean isTerminal() { return terminal; }
    public void setTerminal(boolean terminal) { this.terminal = terminal; }

    public boolean isEditableByCreator() { return editableByCreator; }
    public void setEditableByCreator(boolean editableByCreator) { this.editableByCreator = editableByCreator; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}