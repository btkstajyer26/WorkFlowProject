package btk.staj.WorkFlowProject.workflow.entity;

import jakarta.persistence.*;

/**
 * records.status kolonundaki sabit degerlerin (bkz. V1, chk_records_status)
 * veritabani karsiligi. V12 migration ile chk_records_status kaldirilip
 * bu tabloya FK baglanacak (DB-5); records.status kolonu yine de VARCHAR
 * olarak kalir, status_id'ye donusmez.
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

    @Column(length = 255)
    private String description;

    @Column(name = "is_terminal", nullable = false)
    private boolean terminal;

    public WorkflowStatusEntity() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isTerminal() { return terminal; }
    public void setTerminal(boolean terminal) { this.terminal = terminal; }
}