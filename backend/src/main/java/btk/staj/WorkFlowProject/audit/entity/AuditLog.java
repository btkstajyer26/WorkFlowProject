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

    @Column(name = "record_id", updatable = false)
    private UUID recordId;

    @Column(name = "user_id", updatable = false)
    private UUID userId;

    @Column(name = "role_id", updatable = false)
    private Integer roleId;

    @Column(nullable = false, length = 50, updatable = false)
    private String action;

    @Column(name = "previous_status", length = 50, updatable = false)
    private String previousStatus;

    @Column(name = "new_status", length = 50, updatable = false)
    private String newStatus;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String comment;

    @Column(name = "http_method", length = 10, updatable = false)
    private String httpMethod;

    @Column(name = "request_path", length = 512, updatable = false)
    private String requestPath;

    @Column(name = "http_status", updatable = false)
    private Integer httpStatus;

    @Column(name = "error_code", length = 80, updatable = false)
    private String errorCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;


    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
