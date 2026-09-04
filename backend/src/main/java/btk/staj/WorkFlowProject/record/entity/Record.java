package btk.staj.WorkFlowProject.record.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
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

    // 🔑 id: Primary Key, UUID.
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // title: varchar(255)
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    // description: text, boş bırakılabilir
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // 🔑 category_id: Foreign Key (Integer)
    @Column(name = "category_id", nullable = false)
    private Integer categoryId;

    // 🔑 status: tip güvenli enum olarak tutuluyor, varchar(50)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private RecordStatus status;

    // 🔑 created_by: Evrağı oluşturan kullanıcının UUID'si
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    // 🔑 assigned_to: nullable
    @Column(name = "assigned_to")
    private UUID assignedTo;

    // 🔑 last_deputy_id: nullable
    @Column(name = "last_deputy_id")
    private UUID lastDeputyId;

// ---- V21 (TASLAK) ile eklendi ----
    // Kaydin kisiye DEGIL departmana atanmasi. assignedTo ile ayni anda
    // dolu olamaz (bkz. chk_records_assignment_exclusive, V21).
    // ADR-0006 kapanana kadar bu alani YAZAN kod yoktur - yalniz kolon
    // ve kisit hazir.
    @Column(name = "assigned_department_id")
    private Integer assignedDepartmentId;

    // Not: Record.java @Getter/@Setter (Lombok) kullaniyor - bu alan
    // icin ayrica getter/setter yazmana gerek YOK, Lombok otomatik uretecek.

    
    // Kayit Calisana geri gonderildigi anda icerigin dondurulmus kopyasi.
    // Baskan Yardimcisi, evrak duzeltmedeyken canli icerigi degil bunu gorur
    // (bkz. V9 migration ve RecordAccessPolicy.seesRecordAsOfHandoff).
    // Hic geri gonderilmemis kayitlarda bos kalir.
    @Column(name = "snapshot_title", length = 255)
    private String snapshotTitle;

    @Column(name = "snapshot_description", columnDefinition = "TEXT")
    private String snapshotDescription;

    @Column(name = "snapshot_category_id")
    private Integer snapshotCategoryId;

    /** Devir ani. Ek dosyalar bu zamana gore suzulur; ayrica kopyalanmazlar. */
    @Column(name = "snapshot_at")
    private LocalDateTime snapshotAt;

    // version: Optimistic Locking için, default 0
    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Integer version = 0;

    // 📍 created_at: Hibernate otomatik atar, sonradan değiştirilemez
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // updated_at: Hibernate her güncellemede otomatik değiştirir
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // deleted_at: Soft delete için
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}