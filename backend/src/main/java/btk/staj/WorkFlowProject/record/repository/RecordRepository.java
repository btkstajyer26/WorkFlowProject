package btk.staj.WorkFlowProject.record.repository;

import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying; // Eklendi
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.UUID;

public interface RecordRepository extends JpaRepository<Record, UUID>, JpaSpecificationExecutor<Record> {

    // 1. Oluşturan kullanıcıya göre arama (Kayıtlarım Listesi)
    Page<Record> findByCreatedByAndDeletedAtIsNull(UUID userId, Pageable pageable);

    // 2. Kategoriye göre arama
    Page<Record> findByCategoryId(Integer categoryId, Pageable pageable);

    // 3. Duruma göre arama (RecordStatus.java dosyasından enum değerlerini alıyor.)
    Page<Record> findByStatus(RecordStatus status, Pageable pageable);

    // 4. Tarih aralığına göre arama
    Page<Record> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    // 5. Başlığa veya içeriğe göre metin tabanlı arama
    @Query("SELECT r FROM Record r WHERE LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(r.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Record> searchByTitleOrDescription(@Param("keyword") String keyword, Pageable pageable);

    // 6. Eski kullanıcının (Başkan, Bşk. Yrd. vb.) üzerindeki tüm kayıtları yeni kullanıcıya devretme
    @Modifying
    @Query("UPDATE Record r SET r.assignedTo = :yeniKullaniciId WHERE r.assignedTo = :eskiKullaniciId")
    int devretBekleyenIsleri(@Param("eskiKullaniciId") UUID eskiKullaniciId, @Param("yeniKullaniciId") UUID yeniKullaniciId);
}