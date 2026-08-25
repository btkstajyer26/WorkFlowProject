package btk.staj.WorkFlowProject.audit.repository;

import btk.staj.WorkFlowProject.audit.dto.AuditLogResponse;
import btk.staj.WorkFlowProject.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Denetim izi append-only'dir (sartname §4.2 "silinemez"). Bu yuzden
 * {@code JpaRepository} genisletilmez: o arayuz {@code delete},
 * {@code deleteAll} ve {@code deleteById} metotlarini otomatik acardi. Burada
 * yalnizca yazma ve okuma metotlari tanimlanir, silme API'de hic olusmaz.
 */
public interface AuditLogRepository extends Repository<AuditLog, UUID> {

    AuditLog save(AuditLog auditLog);

    Optional<AuditLog> findById(UUID id);

    /**
     * Bir evragin islem gecmisi, kullanici ve rol adlari cozulmus halde.
     *
     * <p>Adlar tek sorguda birlestirilir. Satir basina ayrica kullanici/rol
     * sorgulansaydi N+1 olusurdu; sartname (§6.2) bundan kacinmayi istiyor.
     * {@code audit_logs} ile {@code users}/{@code roles} arasinda JPA iliskisi
     * tanimli olmadigi icin birlestirme acik {@code JOIN ... ON} ile yapilir.
     */
    @Query("""
            SELECT new btk.staj.WorkFlowProject.audit.dto.AuditLogResponse(
                       a.id,
                       a.recordId,
                       a.userId,
                       CONCAT(u.firstName, ' ', u.lastName),
                       a.roleId,
                       r.name,
                       a.action,
                       a.previousStatus,
                       a.newStatus,
                       a.comment,
                       a.httpMethod,
                       a.requestPath,
                       a.httpStatus,
                       a.errorCode,
                       a.createdAt)
            FROM AuditLog a
            JOIN User u ON u.id = a.userId
            JOIN Role r ON r.id = a.roleId
            WHERE a.recordId = :recordId
            ORDER BY a.createdAt ASC
            """)
    List<AuditLogResponse> findHistoryByRecordId(@Param("recordId") UUID recordId);

    /**
     * Admin panelindeki evrak + admin istek loglari. user_id/role_id
     * giris basarisizliginde bos olabilecegi icin LEFT JOIN kullanilir.
     */
    @Query(value = """
            SELECT new btk.staj.WorkFlowProject.audit.dto.AuditLogResponse(
                       a.id,
                       a.recordId,
                       a.userId,
                       CONCAT(u.firstName, ' ', u.lastName),
                       a.roleId,
                       r.name,
                       a.action,
                       a.previousStatus,
                       a.newStatus,
                       a.comment,
                       a.httpMethod,
                       a.requestPath,
                       a.httpStatus,
                       a.errorCode,
                       a.createdAt)
            FROM AuditLog a
            LEFT JOIN User u ON u.id = a.userId
            LEFT JOIN Role r ON r.id = a.roleId
            ORDER BY a.createdAt DESC
            """,
            countQuery = "SELECT count(a) FROM AuditLog a")
    Page<AuditLogResponse> findAllWithNames(Pageable pageable);

    /**
     * Bir kullanicinin yaptigi islemler. Kullanici omru boyunca sinirsiz
     * buyuyebilecegi icin sayfalanarak donulur (sartname §6.2 Pageable).
     */
    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
