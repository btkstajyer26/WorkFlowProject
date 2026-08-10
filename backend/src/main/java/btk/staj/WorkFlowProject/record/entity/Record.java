package btk.staj.WorkFlowProject.record.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Record {

    // 🔑 id: Anahtar işareti var, Primary Key ve UUID.
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // title: varchar(255)
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    // description: text
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // 🔑 category_id: Başka bir tabloya bağlı (Foreign Key) olduğu için Integer
    @Column(name = "category_id", nullable = false)
    private Integer categoryId;

    // 🔑 status: varchar(50) - Taslak, Onaylandı vb. durumlar için.
    @Column(name = "status", nullable = false, length = 50)
    private String status;

    // 🔑 created_by: Evrağı kim oluşturdu? User tablosunun UUID'si.
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    // 🔑 assigned_to (uuid?): Soru işareti var, yani boş olabilir (nullable = true)
    @Column(name = "assigned_to")
    private UUID assignedTo;

    // 🔑 last_deputy_id (uuid?): Soru işareti var, boş olabilir.
    @Column(name = "last_deputy_id")
    private UUID lastDeputyId;

    // version: Aynı anda iki kişi güncellerse veri ezilmesin diye versiyon kontrolü
    @Version
    @Column(name = "version")
    private Integer version;

    // 📍 created_at: Ne zaman oluşturuldu?
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    // updated_at (timestamp?): Ne zaman güncellendi? Boş olabilir.
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // deleted_at (timestamp?): Soft delete (kalıcı silmeme) için kullanılıyor.
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // --- OTOMATİK İŞLEMLER ---

    // Veritabanına ilk kez kaydedilmeden HEMEN ÖNCE çalışır
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Veritabanında bir güncelleme yapılmadan HEMEN ÖNCE çalışır
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}