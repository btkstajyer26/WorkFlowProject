package btk.staj.WorkFlowProject.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Kullanici ve rol degisikliklerinin ayri denetim izi. {@code audit_logs} tablosu
 * {@code record_id} zorunlu oldugu icin evraktan bagimsiz Admin islemleri oraya
 * yazilamaz.
 *
 * <p>{@link AuditLog} ile ayni degismezlik kurallarina tabidir.
 */
@Entity
@Table(name = "user_audit_logs")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class UserAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false)
    private UUID id;

    @Column(name = "target_user_id", nullable = false, updatable = false)
    private UUID targetUserId;

    // Ilk Admin bootstrap isleminde NULL olabilir (action = 'BOOTSTRAP_ADMIN_CREATED').
    @Column(name = "performed_by", updatable = false)
    private UUID performedBy;

    @Column(nullable = false, length = 50, updatable = false)
    private String action;

    @Column(name = "previous_role_id", updatable = false)
    private Integer previousRoleId;

    @Column(name = "new_role_id", updatable = false)
    private Integer newRoleId;

    @Column(name = "previous_active", updatable = false)
    private Boolean previousActive;

    @Column(name = "new_active", updatable = false)
    private Boolean newActive;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
