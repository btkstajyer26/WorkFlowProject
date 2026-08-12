package btk.staj.WorkFlowProject.audit.repository;

import btk.staj.WorkFlowProject.audit.dto.UserAuditLogResponse;
import btk.staj.WorkFlowProject.audit.entity.UserAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * {@code audit_logs} gibi append-only'dir; silme metotlari bilerek
 * tanimlanmamistir (bkz. {@link AuditLogRepository}).
 */
public interface UserAuditLogRepository extends Repository<UserAuditLog, UUID> {

    UserAuditLog save(UserAuditLog userAuditLog);

    Optional<UserAuditLog> findById(UUID id);

    /** Belirli bir kullanici uzerinde yapilan tum islemler, eskiden yeniye. */
    List<UserAuditLog> findByTargetUserIdOrderByCreatedAtAsc(UUID targetUserId);

    /** Belirli bir yoneticinin yaptigi islemler; sinirsiz buyuyebilir, sayfalanir. */
    Page<UserAuditLog> findByPerformedByOrderByCreatedAtDesc(UUID performedBy, Pageable pageable);

    /**
     * Admin panelindeki "Kullanici ve rol islemleri" gorunumu: tum kullanici
     * denetim izi, hedef/eyleyen kullanicilarin ve rollerin adlari cozulmus
     * halde, en yeniden eskiye, sayfali. {@code performedBy} bootstrap
     * senaryosunda NULL olabildigi icin o alanla LEFT JOIN yapilir.
     */
    @Query(value = """
            SELECT new btk.staj.WorkFlowProject.audit.dto.UserAuditLogResponse(
                       l.id,
                       l.targetUserId,
                       CONCAT(tu.firstName, ' ', tu.lastName),
                       l.performedBy,
                       CONCAT(pu.firstName, ' ', pu.lastName),
                       l.action,
                       l.previousRoleId,
                       pr.name,
                       l.newRoleId,
                       nr.name,
                       l.previousActive,
                       l.newActive,
                       l.comment,
                       l.createdAt)
            FROM UserAuditLog l
            JOIN User tu ON tu.id = l.targetUserId
            LEFT JOIN User pu ON pu.id = l.performedBy
            LEFT JOIN Role pr ON pr.id = l.previousRoleId
            LEFT JOIN Role nr ON nr.id = l.newRoleId
            ORDER BY l.createdAt DESC
            """,
            countQuery = "SELECT count(l) FROM UserAuditLog l")
    Page<UserAuditLogResponse> findAllWithNames(Pageable pageable);
}