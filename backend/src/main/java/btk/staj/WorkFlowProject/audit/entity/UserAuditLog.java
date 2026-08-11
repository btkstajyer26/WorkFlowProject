package btk.staj.WorkFlowProject.audit.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "target_user_id", nullable = false)
    private UUID targetUserId;

    // İlk Admin bootstrap işleminde NULL olabilir
    @Column(name = "performed_by")
    private UUID performedBy;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "previous_role_id")
    private Integer previousRoleId;

    @Column(name = "new_role_id")
    private Integer newRoleId;

    @Column(name = "previous_active")
    private Boolean previousActive;

    @Column(name = "new_active")
    private Boolean newActive;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}