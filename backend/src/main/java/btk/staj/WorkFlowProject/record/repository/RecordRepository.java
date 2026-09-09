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
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.Optional;
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
    //
    // B03 fix: version artik artiyor. Toplu JPQL UPDATE'ler Hibernate'in optimistic
    // locking mekanizmasini (UPDATE ... WHERE id = ? AND version = ?) BYPASS eder -
    // versiyonu artirmazsak, eski snapshot'i elinde tutan baska bir transaction'in
    // yazisi hicbir catisma yakalanmadan basariyla gecer.
    //
    // clearAutomatically = true: bu sorgudan etkilenen Record'larin persistence
    // context'teki eski (bayat) hallerini temizler. flushAutomatically = true:
    // sorgu calismadan once bekleyen degisiklikleri flush eder.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Record r SET r.assignedTo = :yeniKullaniciId, r.version = r.version + 1 WHERE r.assignedTo = :eskiKullaniciId")
    int devretBekleyenIsleri(@Param("eskiKullaniciId") UUID eskiKullaniciId, @Param("yeniKullaniciId") UUID yeniKullaniciId);

    // 7. İş M5 fix: Bşk. Yrd. koltuğu el değiştirdiğinde, eski kullanıcıyı "son Bşk. Yrd."
    // olarak referanslayan kayıtları da güncelle. Aksi halde BASKAN_YARDIMCISINA_GERI_GONDER
    // işlemi artık Bşk. Yrd. olmayan eski kullanıcıyı hedeflemeye çalışıp hata veriyor.
    //
    // B03 fix: ayni gerekce - version artik artiyor, ayni clearAutomatically/
    // flushAutomatically korumasi.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Record r SET r.lastDeputyId = :yeniKullaniciId, r.version = r.version + 1 WHERE r.lastDeputyId = :eskiKullaniciId")
    int updateLastDeputyId(@Param("eskiKullaniciId") UUID eskiKullaniciId, @Param("yeniKullaniciId") UUID yeniKullaniciId);

    // 8. Aktif (silinmemis) kaydin tekil yukleyicisi (B08).
    // Okuma yollari bu filtreyi zaten uyguluyordu; degistirme yollari uygulamiyordu.
    // Filtre uc yere kopyalanmak yerine tek bir sorguda toplanir.
    Optional<Record> findByIdAndDeletedAtIsNull(UUID id);

    // 9. Satir kilidiyle yukleme (B04).
    // Dosya ekleme/silme, durum kontrolu ile yazim arasinda kaydin degismedigini
    // garanti edebilmek icin bu metodu kullanir: kilit transaction sonuna kadar
    // tutuldugundan araya giren workflow gecisi ya bekler ya da bizden once
    // commit edip bizim taze durumu gormemizi saglar.
    // Ayni kalip: RoleRepository.findByIdForUpdate, UserRepository.findByIdForUpdate.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Record r WHERE r.id = :id")
    Optional<Record> findByIdForUpdate(@Param("id") UUID id);

}