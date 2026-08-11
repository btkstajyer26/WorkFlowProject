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
 * Sartnamedeki silinemez denetim izi (§4.2). Kayit bir kez yazilir, sonra
 * degistirilemez:
 *
 * <ul>
 *   <li>{@link Immutable} — Hibernate bu entity icin hic UPDATE uretmez,</li>
 *   <li>kolonlar {@code updatable = false},</li>
 *   <li>setter yok; nesne yalnizca {@code builder()} ile kurulur,</li>
 *   <li>silme metotlari repository arayuzunde hic tanimlanmaz
 *       (bkz. {@code AuditLogRepository}).</li>
 * </ul>
 */
@Entity
@Table(name = "audit_logs")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false)
    private UUID id;

    @Column(name = "record_id", nullable = false, updatable = false)
    private UUID recordId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "role_id", nullable = false, updatable = false)
    private Integer roleId;

    @Column(nullable = false, length = 50, updatable = false)
    private String action;

    @Column(name = "previous_status", length = 50, updatable = false)
    private String previousStatus;

    @Column(name = "new_status", nullable = false, length = 50, updatable = false)
    private String newStatus;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Cagiran taraf islem zamanini kendisi verebilir (onay akisi gecisin
     * gerceklestigi ani tasiyor); vermediyse yazma ani kullanilir.
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
