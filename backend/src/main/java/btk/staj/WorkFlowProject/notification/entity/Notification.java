package btk.staj.WorkFlowProject.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import btk.staj.WorkFlowProject.enums.NotificationType;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private java.util.UUID id;

    @Column(name = "user_id", nullable = false)
    private java.util.UUID userId;

    @Column(name = "record_id", nullable = false)
    private java.util.UUID recordId;

    @Column(nullable = false, length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private btk.staj.WorkFlowProject.enums.NotificationType notificationType;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    // 1. Boş Constructor (JPA için zorunlu)
    public Notification() {
    }

    // 2. Dolu Constructor
    public Notification(java.util.UUID userId, java.util.UUID recordId, String message, btk.staj.WorkFlowProject.enums.NotificationType notificationType) {
        this.userId = userId;
        this.recordId = recordId;
        this.message = message;
        this.notificationType = notificationType;
        this.isRead = false;
    }

    // 3. Getter ve Setter Metotları
    public java.util.UUID getId() { return id; }
    public void setId(java.util.UUID id) { this.id = id; }

    public java.util.UUID getUserId() { return userId; }
    public void setUserId(java.util.UUID userId) { this.userId = userId; }

    public java.util.UUID getRecordId() { return recordId; }
    public void setRecordId(java.util.UUID recordId) { this.recordId = recordId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public btk.staj.WorkFlowProject.enums.NotificationType getNotificationType() { return notificationType; }
    public void setNotificationType(btk.staj.WorkFlowProject.enums.NotificationType notificationType) { this.notificationType = notificationType; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
}