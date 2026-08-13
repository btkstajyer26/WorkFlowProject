package btk.staj.WorkFlowProject.record.entity; 

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "record_notes",
    uniqueConstraints = {
        // Her kullanıcının bir kayıtta tek bir notu olabileceği kısıtlaması
        @UniqueConstraint(
            name = "uq_record_notes_record_author", 
            columnNames = {"record_id", "author_id"}
        )
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecordNote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "record_id", nullable = false)
    private UUID recordId;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "author_role_id", nullable = false)
    private Integer authorRoleId;

    @NotBlank(message = "Çalışma notu boş bırakılamaz")
    @Size(max = 1000, message = "Çalışma notu en fazla 1000 karakter olabilir")
    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    // Eşzamanlı güncellemelerde 409 NOTE_VERSION_CONFLICT fırlatmak için JPA'nın kendi mekanizması
    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}