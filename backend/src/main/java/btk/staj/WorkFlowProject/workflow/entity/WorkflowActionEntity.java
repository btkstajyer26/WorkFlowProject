package btk.staj.WorkFlowProject.workflow.entity;

import jakarta.persistence.*;

/**
 * TransitionRules.java'daki WorkflowAction enum degerlerinin (GONDER,
 * ONAYLA, REDDET vb.) veritabani karsiligi.
 *
 * Isim gecicidir: workflow.statemachine paketindeki WorkflowAction enum'u
 * ile kavramsal cakisma var, port imzasi toplantisinda kesinlesecek.
 */
@Entity
@Table(name = "workflow_actions")
public class WorkflowActionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(length = 255)
    private String description;

    public WorkflowActionEntity() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}