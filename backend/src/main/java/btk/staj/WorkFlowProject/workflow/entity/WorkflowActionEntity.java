package btk.staj.WorkFlowProject.workflow.entity;

import jakarta.persistence.*;

/**
 * TransitionRules.java'daki WorkflowAction enum degerlerinin veritabani
 * karsiligi. bkz. DB_1_VERI_MODELI_SOZLESMESI.md SS6.5.
 *
 * ONEMLI: target_strategy ve expected_target_role_id burada YOKTUR - sozlesme
 * bunlarin workflow_transitions'a ait oldugunu acikca belirtiyor (ayni aksiyon
 * farkli gecislerde farkli hedefe gidebilir).
 *
 * Isim gecicidir, port imzasi toplantisinda kesinlesecek.
 */
@Entity
@Table(name = "workflow_actions")
public class WorkflowActionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 60)
    private String name;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    // Yorum zorunlulugu geciste degil aksiyonda tutulur (sozlesme SS8):
    // iki farkli aktorun kullandigi CALISANA_GERI_GONDER ayni kurali paylasir.
    @Column(name = "comment_required", nullable = false)
    private boolean commentRequired;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    public WorkflowActionEntity() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public boolean isCommentRequired() { return commentRequired; }
    public void setCommentRequired(boolean commentRequired) { this.commentRequired = commentRequired; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}